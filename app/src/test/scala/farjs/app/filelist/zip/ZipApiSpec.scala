package farjs.app.filelist.zip

import farjs.app.filelist.fs.MockChildProcess
import farjs.filelist.api.{FileListDir, FileListItem}
import scommons.nodejs.ChildProcess.ChildProcessOptions
import scommons.nodejs.raw
import scommons.nodejs.test.AsyncTestSpec

import scala.concurrent.Future
import scala.scalajs.js

class ZipApiSpec extends AsyncTestSpec {

  private val entriesF = Future.successful(List(
    ZipEntry("", "dir 1", isDir = true, datetimeMs = 1.0),
    ZipEntry("", "file 1", size = 2.0, datetimeMs = 3.0),
    ZipEntry("dir 1", "dir 2", isDir = true, datetimeMs = 4.0),
    ZipEntry("dir 1/dir 2", "file 2", size = 5.0, datetimeMs = 6.0)
  ))

  //noinspection TypeAnnotation
  class ChildProcess {
    val exec = mockFunction[String, Option[ChildProcessOptions],
      (raw.ChildProcess, Future[(js.Object, js.Object)])]

    val childProcess = new MockChildProcess(
      execMock = exec
    )
  }

  it should "return root dir content when readDir(.)" in {
    //given
    val filePath = "/dir/filePath.zip"
    val rootPath = "zip://filePath.zip"
    val api = new ZipApi(filePath, rootPath, entriesF)
    
    //when
    val resultF = api.readDir(None, FileListItem.currDir.name)

    //then
    resultF.map(inside(_) { case FileListDir(path, isRoot, items) =>
      path shouldBe rootPath
      isRoot shouldBe false
      items shouldBe List(
        FileListItem("dir 1", isDir = true, mtimeMs = 1.0),
        FileListItem("file 1", size = 2.0, mtimeMs = 3.0)
      )
    })
  }

  it should "return root dir content when readDir(..)" in {
    //given
    val filePath = "/dir/filePath.zip"
    val rootPath = "zip://filePath.zip"
    val api = new ZipApi(filePath, rootPath, entriesF)
    
    //when
    val resultF = api.readDir(Some(s"$rootPath/dir 1/dir 2"), FileListItem.up.name)

    //then
    resultF.map(inside(_) { case FileListDir(path, isRoot, items) =>
      path shouldBe s"$rootPath/dir 1"
      isRoot shouldBe false
      items shouldBe List(
        FileListItem("dir 2", isDir = true, mtimeMs = 4.0)
      )
    })
  }

  it should "return sub-dir content when readDir" in {
    //given
    val filePath = "/dir/filePath.zip"
    val rootPath = "zip://filePath.zip"
    val api = new ZipApi(filePath, rootPath, entriesF)
    
    //when
    val resultF = api.readDir(Some(s"$rootPath/dir 1"), "dir 2")

    //then
    resultF.map(inside(_) { case FileListDir(path, isRoot, items) =>
      path shouldBe s"$rootPath/dir 1/dir 2"
      isRoot shouldBe false
      items shouldBe List(
        FileListItem("file 2", size = 5.0, mtimeMs = 6.0)
      )
    })
  }

  it should "call unzip and parse output when readZip" in {
    //given
    val childProcess = new ChildProcess
    val filePath = "/dir/filePath.zip"
    val output =
      """Archive:  /test/dir/file.zip
        |  Length      Date    Time    Name
        |---------  ---------- -----   ----
        |        1  06-28-2019 16:11   test/dir/file.txt
        |---------                     -------
        |        2                     18 files
        |""".stripMargin
    val result: (js.Object, js.Object) = (output.asInstanceOf[js.Object], new js.Object)

    //then
    childProcess.exec.expects(*, *).onCall { (command, options) =>
      command shouldBe s"""unzip -l "$filePath""""
      assertObject(options.get, new ChildProcessOptions {
        override val windowsHide = true
      })

      (null, Future.successful(result))
    }

    //when
    val resultF = ZipApi.readZip(childProcess.childProcess, filePath)

    //then
    resultF.map { res =>
      res shouldBe List(
        ZipEntry("test/dir", "file.txt", size = 1, datetimeMs = js.Date.parse("2019-06-28T16:11:00"))
      )
    }
  }
}
