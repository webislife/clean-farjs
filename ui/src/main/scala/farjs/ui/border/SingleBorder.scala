package farjs.ui.border

import scommons.react._

object SingleBorder extends FunctionComponent[SingleBorderProps] {

  private[border] var horizontalLineComp: UiComponent[HorizontalLineProps] = HorizontalLine
  private[border] var verticalLineComp: UiComponent[VerticalLineProps] = VerticalLine
  
  protected def render(compProps: Props): ReactElement = {
    val props = compProps.plain

    <.>()(
      <(horizontalLineComp())(^.plain := HorizontalLineProps(
        left = 0,
        top = 0,
        length = props.width,
        lineCh = SingleChars.horizontal,
        style = props.style,
        startCh = SingleChars.topLeft,
        endCh = SingleChars.topRight
      ))(),
      
      <(verticalLineComp())(^.wrapped := VerticalLineProps(
        pos = (0, 1),
        length = props.height - 2,
        lineCh = SingleChars.vertical,
        style = props.style
      ))(),
      
      <(verticalLineComp())(^.wrapped := VerticalLineProps(
        pos = (props.width - 1, 1),
        length = props.height - 2,
        lineCh = SingleChars.vertical,
        style = props.style
      ))(),

      <(horizontalLineComp())(^.plain := HorizontalLineProps(
        left = 0,
        top = props.height - 1,
        length = props.width,
        lineCh = SingleChars.horizontal,
        style = props.style,
        startCh = SingleChars.bottomLeft,
        endCh = SingleChars.bottomRight
      ))()
    )
  }
}
