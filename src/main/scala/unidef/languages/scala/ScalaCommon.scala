package unidef.languages.scala

import unidef.common.ty.{TyAny, TyBoolean, TyInteger, TyList, TyNamed, TyNode, TyOptional, TyString, TyUnit, TypeEncoder}
import unidef.utils.TextTool

class ScalaCommon() extends TypeEncoder[String] {
  override def encode(ty: TyNode): Option[String] =
    ty match {
      case _: TyInteger => Some("Int")
      case _: TyString => Some("String")
      case _: TyUnit => Some("Unit")
      case _: TyBoolean => Some("Boolean")
      case t: TyOptional => encode(t.getContent).map(x => s"Option[${x}]")
      case _: TyAny => Some("Any")
      case t: TyList => encode(t.getContent).map(x => s"List[${x}]")
      case TyNamed("this.type") => Some("this.type")
      case TyNamed(name) => Some(TextTool.toPascalCase(name))
      case TyNode => Some("TyNode")
      case _ => None
    }
}
