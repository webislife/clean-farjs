package farjs.viewer

import farjs.viewer.ViewerContent._
import org.scalactic.source.Position
import org.scalatest.{Assertion, Succeeded}
import scommons.nodejs.test.AsyncTestSpec
import scommons.react.ReactRef
import scommons.react.blessed._
import scommons.react.test._

import scala.concurrent.{Future, Promise}

class ViewerContentSpec extends AsyncTestSpec with BaseTestSpec with TestRendererUtils {

  ViewerContent.viewerInput = mockUiComponent("ViewerInput")

  //noinspection TypeAnnotation
  class ViewerFileReader {
    val readPrevLines = mockFunction[Int, Double, Double, String, Future[List[(String, Int)]]]
    val readNextLines = mockFunction[Int, Double, String, Future[List[(String, Int)]]]

    val fileReader = new MockViewerFileReader(
      readPrevLinesMock = readPrevLines,
      readNextLinesMock = readNextLines
    )
  }

  //noinspection TypeAnnotation
  class TestContext(implicit pos: Position) {
    
    val inputRef = ReactRef.create[BlessedElement]
    val fileReader = new ViewerFileReader
    val setViewport = mockFunction[Option[ViewerFileViewport], Unit]
    var props = getViewerContentProps(inputRef, fileReader, setViewport)
    var viewport = props.viewport
    val readF = Future.successful("test \nfile content".split('\n').map(c => (c, c.length)).toList)
    fileReader.readNextLines.expects(viewport.height, 0.0, viewport.encoding).returning(readF)
    val renderer = createTestRenderer(<(ViewerContent())(^.wrapped := props)())
  
    setViewport.expects(*).onCall { maybeViewport: Option[ViewerFileViewport] =>
      inside(maybeViewport) { case Some(vp) =>
        viewport = vp
        TestRenderer.act { () =>
          props = props.copy(viewport = vp)
          renderer.update(<(ViewerContent())(^.wrapped := props)())
        }
      }
    }.anyNumberOfTimes()
    assertViewerContent(renderer.root, props, content = "")
  }

  it should "not move viewport if not completed when onWheel(up/down)" in {
    //given
    val inputRef = ReactRef.create[BlessedElement]
    val fileReader = new ViewerFileReader
    val setViewport = mockFunction[Option[ViewerFileViewport], Unit]
    var props = getViewerContentProps(inputRef, fileReader, setViewport)
    var viewport = props.viewport
    val readP = Promise[List[(String, Int)]]()
    fileReader.readNextLines.expects(viewport.height, 0.0, viewport.encoding).returning(readP.future)
    val renderer = createTestRenderer(<(ViewerContent())(^.wrapped := props)())

    setViewport.expects(*).onCall { maybeViewport: Option[ViewerFileViewport] =>
      inside(maybeViewport) { case Some(vp) =>
        viewport = vp
        TestRenderer.act { () =>
          props = props.copy(viewport = vp)
          renderer.update(<(ViewerContent())(^.wrapped := props)())
        }
      }
    }
    assertViewerContent(renderer.root, props, content = "")

    //then
    fileReader.readPrevLines.expects(*, *, *, *).never()
    fileReader.readNextLines.expects(*, *, *).never()

    //when
    findComponentProps(renderer.root, viewerInput).onWheel(true)
    findComponentProps(renderer.root, viewerInput).onWheel(false)
    findComponentProps(renderer.root, viewerInput).onWheel(true)
    findComponentProps(renderer.root, viewerInput).onWheel(false)

    //then
    assertViewerContent(renderer.root, props, content = "")
    readP.success("completed".split('\n').map(c => (c, c.length)).toList)
    eventually {
      assertViewerContent(renderer.root, props,
        """completed
          |""".stripMargin)
    }
  }

  it should "move viewport when onWheel" in {
    //given
    val ctx = new TestContext
    import ctx._

    def check(up: Boolean, lines: Int, position: Double, content: String, expected: String)
             (implicit pos: Position): () => Future[Unit] = { () =>

      val readF = Future.successful(content.split('\n').map(c => (c, c.length)).toList)

      //then
      if (up) fileReader.readPrevLines.expects(lines, position, viewport.size, viewport.encoding).returning(readF)
      else fileReader.readNextLines.expects(lines, position, viewport.encoding).returning(readF)

      //when
      findComponentProps(renderer.root, viewerInput).onWheel(up)

      //then
      eventually(assertViewerContent(renderer.root, props, expected))
    }

    eventually {
      assertViewerContent(renderer.root, props,
        """test 
          |file content
          |""".stripMargin)
    }.flatMap { _ =>
      List(
        //when & then
        check(up = false, lines = 1, position = 17.0, "end",
          """file content
            |end
            |""".stripMargin
        ),
        check(up = true, lines = 1, position = 5.0, "begin",
          """begin
            |file content
            |end
            |""".stripMargin
        )
      ).foldLeft(Future.unit)((res, f) => res.flatMap(_ => f())).map(_ => Succeeded)
    }
  }

  it should "switch encoding when onKeypress(F8)" in {
    //given
    val ctx = new TestContext
    import ctx._

    def check(key: String, content: String, encoding: String, expected: String)
             (implicit pos: Position): () => Future[Unit] = { () =>

      val readF = Future.successful(content.split('\n').map(c => (c, c.length)).toList)

      //then
      fileReader.readNextLines.expects(viewport.height, viewport.position, encoding).returning(readF)

      //when
      findComponentProps(renderer.root, viewerInput).onKeypress(key)

      //then
      eventually(assertViewerContent(renderer.root, props, expected))
    }

    eventually {
      assertViewerContent(renderer.root, props,
        """test 
          |file content
          |""".stripMargin)
    }.flatMap { _ =>
      List(
        //when & then
        check("f8", "reload1", "latin1",
          """reload1
            |""".stripMargin
        ),
        check("f8", "reload2", "utf-8",
          """reload2
            |""".stripMargin
        ),
        check("f8", "reload3", "latin1",
          """reload3
            |""".stripMargin
        )
      ).foldLeft(Future.unit)((res, f) => res.flatMap(_ => f())).map(_ => Succeeded)
    }
  }

  it should "re-load prev page if at the end when onKeypress(down)" in {
    //given
    val inputRef = ReactRef.create[BlessedElement]
    val fileReader = new ViewerFileReader
    val setViewport = mockFunction[Option[ViewerFileViewport], Unit]
    var props = {
      val p = getViewerContentProps(inputRef, fileReader, setViewport)
      p.copy(viewport = p.viewport.copy(size = 10))
    }
    var viewport = props.viewport
    val readF = Future.successful("1\n2\n3\n4\n5\n".split('\n').map(c => (c, c.length + 1)).toList)
    fileReader.readNextLines.expects(viewport.height, 0.0, viewport.encoding).returning(readF)
    val renderer = createTestRenderer(<(ViewerContent())(^.wrapped := props)())

    setViewport.expects(*).onCall { maybeViewport: Option[ViewerFileViewport] =>
      inside(maybeViewport) { case Some(vp) =>
        viewport = vp
        TestRenderer.act { () =>
          props = props.copy(viewport = vp)
          renderer.update(<(ViewerContent())(^.wrapped := props)())
        }
      }
    }.anyNumberOfTimes()
    eventually {
      assertViewerContent(renderer.root, props,
        """1
          |2
          |3
          |4
          |5
          |""".stripMargin)
    }.flatMap { _ =>
      //then
      val resF = Future.successful("2\n3\n4\n5\n\n".split('\n').map(c => (c, c.length + 1)).toList)
      fileReader.readPrevLines.expects(viewport.height, viewport.size, viewport.size, viewport.encoding).returning(resF)
  
      //when
      findComponentProps(renderer.root, viewerInput).onKeypress("down")
  
      //then
      eventually {
        assertViewerContent(renderer.root, props,
          """2
            |3
            |4
            |5
            |""".stripMargin)
      }
    }
  }

  it should "move viewport when onKeypress" in {
    //given
    val ctx = new TestContext
    import ctx._

    def check(key: String, lines: Int, position: Double, content: String, expected: String, noop: Boolean = false)
             (implicit pos: Position): () => Future[Unit] = { () =>

      val readF =
        if (content.isEmpty) Future.successful(Nil)
        else Future.successful(content.split('\n').map(c => (c, c.length)).toList)

      //then
      if (!noop) {
        if (Set("end", "up", "pageup").contains(key)) {
          fileReader.readPrevLines.expects(lines, position, viewport.size, viewport.encoding).returning(readF)
        }
        else {
          fileReader.readNextLines.expects(lines, position, viewport.encoding).returning(readF)
        }
      }

      //when
      findComponentProps(renderer.root, viewerInput).onKeypress(key)

      //then
      eventually(assertViewerContent(renderer.root, props, expected))
    }

    eventually {
      assertViewerContent(renderer.root, props,
        """test 
          |file content
          |""".stripMargin)
    }.flatMap { _ =>
      List(
        //when & then
        check(key = "C-r", lines = viewport.height, position = 0.0, "new content",
          """new content
            |""".stripMargin
        ),
        check(key = "end", lines = viewport.height, position = viewport.size, "ending",
          """ending
            |""".stripMargin
        ),
        check(key = "home", lines = viewport.height, position = 0.0, "beginning",
          """beginning
            |""".stripMargin
        ),
        check(key = "up", lines = 1, position = 0.0, "already at the beginning",
          """beginning
            |""".stripMargin,
          noop = true
        ),
        check(key = "down", lines = 1, position = 9, "next line 1",
          """next line 1
            |""".stripMargin
        ),
        check(key = "down", lines = 1, position = 20, "",
          """next line 1
            |""".stripMargin
        ),
        check(key = "down", lines = 1, position = 20, "line2",
          """line2
            |""".stripMargin
        ),
        check(key = "down", lines = 1, position = 25, "out of file size",
          """line2
            |""".stripMargin,
          noop = true
        ),
        check(key = "up", lines = 1, position = 20, "prev line",
          """prev line
            |line2
            |""".stripMargin
        ),
        check(key = "up", lines = 1, position = 11, "",
          """prev line
            |line2
            |""".stripMargin
        ),
        check(key = "pageup", lines = viewport.height, position = 11, "1\n2\n3\n4",
          """1
            |2
            |3
            |4
            |prev line
            |""".stripMargin
        ),
        check(key = "pagedown", lines = viewport.height, position = 20, "next paaaaaaage",
          """next paaaaaa
            |""".stripMargin
        ),
        check(key = "left", lines = viewport.height, position = 20, "",
          """next paaaaaa
            |""".stripMargin,
          noop = true
        ),
        check(key = "right", lines = viewport.height, position = 20, "",
          """ext paaaaaaa
            |""".stripMargin,
          noop = true
        ),
        check(key = "right", lines = viewport.height, position = 20, "",
          """xt paaaaaaag
            |""".stripMargin,
          noop = true
        ),
        check(key = "left", lines = viewport.height, position = 20, "",
          """ext paaaaaaa
            |""".stripMargin,
          noop = true
        ),
        check(key = "f2", lines = viewport.height, position = 20, "loooooooooooong line\n",
          """looooooooooo
            |ong line
            |""".stripMargin
        ),
        check(key = "up", lines = 1, position = 20, "prev liiiiiiiiine 1\n",
          """iiine 1
            |looooooooooo
            |ong line
            |""".stripMargin
        ),
        check(key = "home", lines = viewport.height, position = 0.0, "beginning",
          """beginning
            |""".stripMargin
        ),
        check(key = "down", lines = 1, position = 9, "next liiiiiiiiine 1\n",
          """next liiiiii
            |""".stripMargin
        ),
        check(key = "right", lines = viewport.height, position = 9, "",
          """ext liiiiii
            |""".stripMargin,
          noop = true
        ),
        check(key = "f2", lines = viewport.height, position = 9, "next liiiiiiiiine 1\n",
          """ext liiiiiii
            |""".stripMargin
        )
      ).foldLeft(Future.unit)((res, f) => res.flatMap(_ => f())).map(_ => Succeeded)
    }
  }

  it should "do nothing when onKeypress(unknown)" in {
    //given
    val ctx = new TestContext
    import ctx._
    eventually {
      assertViewerContent(renderer.root, props,
        """test 
          |file content
          |""".stripMargin)
    }.flatMap { _ =>
      //when
      findComponentProps(renderer.root, viewerInput).onKeypress("unknown")
  
      //then
      eventually {
        assertViewerContent(renderer.root, props,
          """test 
            |file content
            |""".stripMargin)
      }
    }
  }

  it should "reload current page if props has changed" in {
    //given
    val ctx = new TestContext
    import ctx._
    eventually {
      assertViewerContent(renderer.root, props,
        """test 
          |file content
          |""".stripMargin)
    }.flatMap { _ =>
      val updatedProps = props.copy(
        viewport = viewport.copy(
          encoding = "utf-16",
          size = 11,
          width = 61,
          height = 21
        )
      )
      updatedProps.viewport.encoding should not be viewport.encoding
      updatedProps.viewport.size should not be viewport.size
      updatedProps.viewport.width should not be viewport.width
      updatedProps.viewport.height should not be viewport.height
      val content2 = "test file content2"
      val read2F = Future.successful(List((content2, content2.length)))

      //then
      fileReader.readNextLines.expects(updatedProps.viewport.height, 0.0, updatedProps.viewport.encoding)
        .returning(read2F)

      //when
      TestRenderer.act { () =>
        renderer.update(<(ViewerContent())(^.wrapped := updatedProps)())
      }

      //then
      eventually {
        assertViewerContent(renderer.root, updatedProps,
          """test file content2
            |""".stripMargin)
      }
    }
  }
  
  it should "not reload current page if props hasn't changed" in {
    //given
    val ctx = new TestContext
    import ctx._
    eventually {
      assertViewerContent(renderer.root, props,
        """test 
          |file content
          |""".stripMargin)
    }.flatMap { _ =>
      val updatedProps = props.copy()
      updatedProps should not be theSameInstanceAs (props)
      updatedProps.viewport.encoding shouldBe viewport.encoding
      updatedProps.viewport.size shouldBe viewport.size
      updatedProps.viewport.width shouldBe viewport.width
      updatedProps.viewport.height shouldBe viewport.height

      //when
      TestRenderer.act { () =>
        renderer.update(<(ViewerContent())(^.wrapped := updatedProps)())
      }

      //then
      eventually {
        assertViewerContent(renderer.root, updatedProps,
          """test 
            |file content
            |""".stripMargin)
      }
    }
  }
  
  it should "call setViewport when non-empty file" in {
    //given
    val inputRef = ReactRef.create[BlessedElement]
    val fileReader = new ViewerFileReader
    val setViewport = mockFunction[Option[ViewerFileViewport], Unit]
    var props = getViewerContentProps(inputRef, fileReader, setViewport)
    var viewport = props.viewport
    val readF = Future.successful("test \nfile content\n".split('\n').map(c => (c, c.length + 1)).toList)
    fileReader.readNextLines.expects(viewport.height, 0.0, viewport.encoding).returning(readF)
    val percent = ((19 / viewport.size) * 100).toInt
    percent shouldBe 76

    //when
    val renderer = createTestRenderer(<(ViewerContent())(^.wrapped := props)())

    //then
    setViewport.expects(*).onCall { maybeViewport: Option[ViewerFileViewport] =>
      inside(maybeViewport) { case Some(vp) =>
        viewport = vp
        TestRenderer.act { () =>
          props = props.copy(viewport = vp)
          renderer.update(<(ViewerContent())(^.wrapped := props)())
        }
      }
    }
    assertViewerContent(renderer.root, props, content = "")
    eventually {
      assertViewerContent(renderer.root, props,
        """test 
          |file content
          |""".stripMargin)

      viewport.progress shouldBe percent
    }
  }
  
  it should "call setViewport when empty file" in {
    //given
    val inputRef = ReactRef.create[BlessedElement]
    val fileReader = new ViewerFileReader
    val setViewport = mockFunction[Option[ViewerFileViewport], Unit]
    var props = {
      val p = getViewerContentProps(inputRef, fileReader, setViewport)
      p.copy(viewport = p.viewport.copy(size = 0))
    }
    var viewport = props.viewport
    val readF = Future.successful("test content".split('\n').map(c => (c, c.length)).toList)
    fileReader.readNextLines.expects(viewport.height, 0.0, viewport.encoding).returning(readF)
    val percent = 0

    //when
    val renderer = createTestRenderer(<(ViewerContent())(^.wrapped := props)())

    //then
    setViewport.expects(*).onCall { maybeViewport: Option[ViewerFileViewport] =>
      inside(maybeViewport) { case Some(vp) =>
        viewport = vp
        TestRenderer.act { () =>
          props = props.copy(viewport = vp)
          renderer.update(<(ViewerContent())(^.wrapped := props)())
        }
      }
    }
    assertViewerContent(renderer.root, props, content = "")
    eventually {
      assertViewerContent(renderer.root, props,
        """test content
          |""".stripMargin)

      viewport.progress shouldBe percent
    }
  }
  
  private def getViewerContentProps(inputRef: ReactRef[BlessedElement],
                                    fileReader: ViewerFileReader,
                                    setViewport: Option[ViewerFileViewport] => Unit) = {
    ViewerContentProps(
      inputRef = inputRef,
      viewport = ViewerFileViewport(
        fileReader = fileReader.fileReader,
        encoding = "utf-8",
        size = 25,
        width = 12,
        height = 5
      ),
      setViewport = setViewport
    )
  }

  private def assertViewerContent(result: TestInstance,
                                  props: ViewerContentProps,
                                  content: String)(implicit pos: Position): Assertion = {

    assertComponents(result.children, List(
      <(viewerInput())(^.assertWrapped(inside(_) {
        case ViewerInputProps(inputRef, _, _) =>
          inputRef shouldBe props.inputRef
      }))(
        <.text(
          ^.rbStyle := ViewerController.contentStyle,
          ^.content := content
        )()
      )
    ))
  }
}
