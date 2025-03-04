package farjs.ui.menu

import farjs.ui.border._
import farjs.ui.theme.Theme
import scommons.react._
import scommons.react.blessed._

case class SubMenuProps(selected: Int,
                        items: List[String],
                        top: Int,
                        left: Int,
                        onClick: Int => Unit)

object SubMenu extends FunctionComponent[SubMenuProps] {

  private[menu] var doubleBorderComp: UiComponent[DoubleBorderProps] = DoubleBorder
  private[menu] var horizontalLineComp: UiComponent[HorizontalLineProps] = HorizontalLine

  protected def render(compProps: Props): ReactElement = {
    val props = compProps.wrapped

    val textWidth = props.items.maxBy(_.length).length
    val width = 2 + textWidth
    val height = 2 + props.items.size
    val theme = Theme.current.popup.menu

    <.box(
      ^.rbClickable := true,
      ^.rbAutoFocus := false,
      ^.rbWidth := width,
      ^.rbHeight := height,
      ^.rbTop := props.top,
      ^.rbLeft := props.left,
      ^.rbShadow := true,
      ^.rbStyle := theme
    )(
      <(doubleBorderComp())(^.plain := DoubleBorderProps(
        width = width,
        height = height,
        style = theme
      ))(),

      props.items.zipWithIndex.map { case (text, index) =>
        if (text == separator) {
          <(horizontalLineComp())(^.key := s"$index", ^.plain := HorizontalLineProps(
            left = 0,
            top = 1 + index,
            length = width,
            lineCh = SingleChars.horizontal,
            style = theme,
            startCh = DoubleChars.leftSingle,
            endCh = DoubleChars.rightSingle
          ))()
        }
        else {
          <.text(
            ^.key := s"$index",
            ^.rbHeight := 1,
            ^.rbLeft := 1,
            ^.rbTop := 1 + index,
            ^.rbClickable := true,
            ^.rbMouse := true,
            ^.rbAutoFocus := false,
            ^.rbStyle := {
              if (props.selected == index) theme.focus.getOrElse(null)
              else theme
            },
            ^.rbOnClick := { _ =>
              props.onClick(index)
            },
            ^.content := text
          )()
        }
      }
    )
  }
  
  val separator = "{SEPARATOR}"
}
