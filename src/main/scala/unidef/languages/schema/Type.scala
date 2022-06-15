package unidef.languages.schema

import io.circe.{Json, JsonNumber, JsonObject}
import io.circe.syntax.*
import unidef.common.ty.*
import unidef.common.ast.*
import unidef.utils.ParseCodeException

import scala.collection.mutable

class Type(val name: String) {
  val fields: mutable.ArrayBuffer[TyField] = mutable.ArrayBuffer.empty
  val equivalent: mutable.ArrayBuffer[TyNode] = mutable.ArrayBuffer.empty
  var commentable: Boolean = false

  def field(name: String, ty: TyNode, required: Boolean = false): Type = {
    fields += TyField(name, ty, None, Some(!required))
    this
  }
  def setCommentable(commentable: Boolean): Type = {
    this.commentable = commentable
    this
  }
  def is(ty: TyNode): Type = {
    equivalent += ty
    this
  }

}
