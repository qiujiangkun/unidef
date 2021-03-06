package unidef.languages.sql

import com.typesafe.scalalogging.Logger
import unidef.common.ast.AstValDef
import unidef.common.NamingConvention
import unidef.common.ty.*
import unidef.utils.TypeEncodeException

import java.util.concurrent.TimeUnit
import scala.collection.mutable

class SqlCommon(naming: NamingConvention = SqlNamingConvention)
    extends TypeDecoder[String]
    with TypeEncoder[String] {

  def convertReal(ty: TyReal): String = ty match {
    case x: TyDecimal if x.precision.isDefined && x.scale.isDefined =>
      s"decimal(${x.precision.get}, ${x.scale.get})"
    case _: TyDecimal => s"decimal"
    case x: TyFloat if x.bitSize.contains(BitSize.B32) => "real"
    case x: TyFloat if x.bitSize.contains(BitSize.B64) => "float" // "double precision"
  }

  def convertInt(ty: TyInteger): String = ty.bitSize match {
    case Some(BitSize.B16) => "smallint"
    case Some(BitSize.B64) => "bigint"
    case Some(x) if x != BitSize.B32 => s"integer($x)"
    case _ => "integer"
  }

  def convertType(ty: TyNode): String = ty match {
    case t: TyNamed => t.ref
    case t => encode(t).getOrElse(throw TypeEncodeException("SQL", t))
  }

  override def encode(ty: TyNode): Option[String] = ty match {
    case t: TyOption => encode(t.value).map(s => s"$s = NULL")
    case t: TyReal => Some(convertReal(t))
    case t: TyOid => Some("oid")
    case t: TyInteger => Some(convertInt(t))
    case t: TyTimeStamp if t.hasTimeZone.contains(true) =>
      Some("timestamp with time zone")
    // case TimeStampType(_, false) => "timestamp without time zone"
    case _: TyTimeStamp => Some("timestamp")
    case _: TyString => Some("text")
    case _: TyStruct => Some("jsonb")
    case x: TyEnum if x.simpleEnum.contains(false) => Some("jsonb")
    case x: TyEnum if x.name.isDefined =>
      x.name
    case _: TyEnum => Some("text")
    case x: TyJsonObject if x.isBinary => Some("jsonb")
    case x: TyJsonObject => Some("json")
    case t: TyJsonAny if t.isBinary => Some("jsonb")
    case t: TyJsonAny => Some("json")
    case _: TyUnit => Some("void")
    case _: TyBoolean => Some("boolean")
    case _: TyByteArray => Some("bytea")
    case _: TyInet => Some("inet")
    case _: TyUuid => Some("uuid")
    case _: TyRecord => Some("record")
    case t: TyList => encode(t.value).map(x => s"${x}[]")
    case _ => None
  }

  override def decode(ty: String): Option[TyNode] = {
    ty match {
      case s"$ty[]" => decode(ty).map(x => Types.list(x))
      case "bigint" | "bigserial" => Some(Types.i64())
      case "integer" | "int" | "serial" => Some(Types.i32())
      case "smallint" => Some(Types.i16())
      case "double precision" | "float" => Some(Types.f64())
      case "real" => Some(Types.f32())
      case "decimal" | "numeric" => Some(TyDecimalImpl(None, None))
      case "timestamp" | "timestamp without time zone" =>
        Some(
          TyTimeStampBuilder().timeUnit(TimeUnit.MILLISECONDS).hasTimeZone(false).build()
        )
      case "timestamp with time zone" =>
        Some(
          TyTimeStampBuilder().timeUnit(TimeUnit.MILLISECONDS).hasTimeZone(true).build()
        )
      case "text" | "varchar" => Some(Types.string())
      case "jsonb" => Some(TyJsonAnyBuilder().isBinary(true).build())
      case "json" => Some(TyJsonAnyBuilder().isBinary(false).build())
      case "void" => Some(Types.unit())
      case "oid" => Some(TyOidImpl())
      case "bool" | "boolean" => Some(Types.bool())
      case "bytea" => Some(TyByteArrayImpl())
      case "inet" => Some(TyInetImpl())
      case "uuid" => Some(TyUuidImpl())
      case "record" => Some(TyRecordImpl())
      case _ => None
    }
  }
  def convertToSqlField(node: AstValDef): SqlField = {
    val attributes = new mutable.StringBuilder()
    if (node.primaryKey.contains(true))
      attributes ++= " PRIMARY KEY"
    // TODO: auto incr
    node.ty match {
      case x: TyOption =>
        attributes ++= " NULL" // optional
        SqlField(
          naming.toFieldName(node.name),
          convertType(x.value),
          attributes.toString
        )
      case _ =>
        attributes ++= " NOT NULL"
        SqlField(
          naming.toFieldName(node.name),
          convertType(node.ty),
          attributes.toString
        )
    }
  }

}
