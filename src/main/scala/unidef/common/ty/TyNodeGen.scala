package unidef.common.ty

trait HasValues() extends TyNode {
  def values: List[TyNode]
}
trait HasDerives() extends TyNode {
  def derives: Option[List[String]]
}
trait HasOk() extends TyNode {
  def ok: TyNode
}
trait HasErr() extends TyNode {
  def err: TyNode
}
trait HasContent() extends TyNode {
  def content: TyNode
}
trait HasAttributes() extends TyNode {
  def attributes: Option[List[String]]
}
trait HasSized() extends TyNode {
  def sized: Option[Boolean]
}
trait HasDefaultNone() extends TyNode {
  def defaultNone: Option[Boolean]
}
trait HasScale() extends TyNode {
  def scale: Option[Int]
}
trait HasName() extends TyNode {
  def name: Option[String]
}
trait HasComment() extends TyNode {
  def comment: Option[String]
}
trait HasMutability() extends TyNode {
  def mutability: Option[Boolean]
}
trait HasFields() extends TyNode {
  def fields: Option[List[TyField]]
}
trait HasDataframe() extends TyNode {
  def dataframe: Option[Boolean]
}
trait HasKey() extends TyNode {
  def key: TyNode
}
trait HasPrecision() extends TyNode {
  def precision: Option[Int]
}
trait HasValue() extends TyNode {
  def value: TyNode
}
trait HasBitSize() extends TyNode {
  def bitSize: Option[BitSize]
}
trait HasSchema() extends TyNode {
  def schema: Option[String]
}
class TyAnyImpl() extends TyAny 
class TyRealImpl() extends TyReal 
class TyFloatImpl(val bitSize: Option[BitSize]) extends TyFloat 
class TySetImpl(val content: TyNode) extends TySet 
class TyOptionalImpl(val content: TyNode) extends TyOptional 
class TyStringImpl() extends TyString 
class TyObjectImpl() extends TyObject 
class TyDecimalImpl(val precision: Option[Int], val scale: Option[Int]) extends TyDecimal 
class TyByteArrayImpl() extends TyByteArray 
class TyInetImpl() extends TyInet 
class TyTupleImpl(val values: List[TyNode]) extends TyTuple 
class TyStructImpl(val name: Option[String], val fields: Option[List[TyField]], val derives: Option[List[String]], val attributes: Option[List[String]], val dataframe: Option[Boolean], val schema: Option[String], val comment: Option[String]) extends TyStruct 
class TyClassImpl() extends TyClass 
class TyRecordImpl() extends TyRecord 
class TyFieldImpl(val name: Option[String], val value: TyNode, val mutability: Option[Boolean], val defaultNone: Option[Boolean]) extends TyField 
class TyMapImpl(val key: TyNode, val value: TyNode) extends TyMap 
class TyUuidImpl() extends TyUuid 
class TyResultImpl(val ok: TyNode, val err: TyNode) extends TyResult 
class TyCharImpl() extends TyChar 
class TyNullImpl() extends TyNull 
class TyNothingImpl() extends TyNothing 
class TyBooleanImpl() extends TyBoolean 
class TyNumericImpl() extends TyNumeric 
class TyIntegerImpl(val bitSize: Option[BitSize], val sized: Option[Boolean]) extends TyInteger 
class TyUnitImpl() extends TyUnit 
class TyListImpl(val content: TyNode) extends TyList 
class TyUndefinedImpl() extends TyUndefined 
trait TyAny() extends TyNode 
trait TyReal() extends TyNode with TyNumeric 
trait TyFloat() extends TyNode with TyReal with HasBitSize {
  def bitSize: Option[BitSize]
}
trait TySet() extends TyNode with HasContent {
  def content: TyNode
}
trait TyOptional() extends TyNode with HasContent {
  def content: TyNode
}
trait TyString() extends TyNode 
trait TyObject() extends TyNode 
trait TyDecimal() extends TyNode with TyReal with HasPrecision with HasScale {
  def precision: Option[Int]
  def scale: Option[Int]
}
trait TyByteArray() extends TyNode 
trait TyInet() extends TyNode 
trait TyTuple() extends TyNode with HasValues {
  def values: List[TyNode]
}
trait TyStruct() extends TyNode with TyClass with HasName with HasFields with HasDerives with HasAttributes with HasDataframe with HasSchema with HasComment {
  def name: Option[String]
  def fields: Option[List[TyField]]
  def derives: Option[List[String]]
  def attributes: Option[List[String]]
  def dataframe: Option[Boolean]
  def schema: Option[String]
  def comment: Option[String]
}
trait TyClass() extends TyNode 
trait TyRecord() extends TyNode 
trait TyField() extends TyNode with HasName with HasValue with HasMutability with HasDefaultNone {
  def name: Option[String]
  def value: TyNode
  def mutability: Option[Boolean]
  def defaultNone: Option[Boolean]
}
trait TyMap() extends TyNode with HasKey with HasValue {
  def key: TyNode
  def value: TyNode
}
trait TyUuid() extends TyNode 
trait TyResult() extends TyNode with HasOk with HasErr {
  def ok: TyNode
  def err: TyNode
}
trait TyChar() extends TyNode 
trait TyNull() extends TyNode 
trait TyNothing() extends TyNode 
trait TyBoolean() extends TyNode 
trait TyNumeric() extends TyNode 
trait TyInteger() extends TyNode with TyNumeric with HasBitSize with HasSized {
  def bitSize: Option[BitSize]
  def sized: Option[Boolean]
}
trait TyUnit() extends TyNode 
trait TyList() extends TyNode with HasContent {
  def content: TyNode
}
trait TyUndefined() extends TyNode 
class TyAnyBuilder() {
  def build(): TyAnyImpl = {
    TyAnyImpl()
  }
}
class TyRealBuilder() {
  def build(): TyRealImpl = {
    TyRealImpl()
  }
}
class TyFloatBuilder() {
  var bitSize: Option[BitSize] = None
  def bitSize(bitSize: BitSize): TyFloatBuilder = {
    this.bitSize = Some(bitSize)
    this
  }
  def build(): TyFloatImpl = {
    TyFloatImpl(bitSize)
  }
}
class TySetBuilder() {
  var content: Option[TyNode] = None
  def content(content: TyNode): TySetBuilder = {
    this.content = Some(content)
    this
  }
  def build(): TySetImpl = {
    TySetImpl(content.get)
  }
}
class TyOptionalBuilder() {
  var content: Option[TyNode] = None
  def content(content: TyNode): TyOptionalBuilder = {
    this.content = Some(content)
    this
  }
  def build(): TyOptionalImpl = {
    TyOptionalImpl(content.get)
  }
}
class TyStringBuilder() {
  def build(): TyStringImpl = {
    TyStringImpl()
  }
}
class TyObjectBuilder() {
  def build(): TyObjectImpl = {
    TyObjectImpl()
  }
}
class TyDecimalBuilder() {
  var precision: Option[Int] = None
  var scale: Option[Int] = None
  def precision(precision: Int): TyDecimalBuilder = {
    this.precision = Some(precision)
    this
  }
  def scale(scale: Int): TyDecimalBuilder = {
    this.scale = Some(scale)
    this
  }
  def build(): TyDecimalImpl = {
    TyDecimalImpl(precision, scale)
  }
}
class TyByteArrayBuilder() {
  def build(): TyByteArrayImpl = {
    TyByteArrayImpl()
  }
}
class TyInetBuilder() {
  def build(): TyInetImpl = {
    TyInetImpl()
  }
}
class TyTupleBuilder() {
  var values: Option[List[TyNode]] = None
  def values(values: List[TyNode]): TyTupleBuilder = {
    this.values = Some(values)
    this
  }
  def build(): TyTupleImpl = {
    TyTupleImpl(values.get)
  }
}
class TyStructBuilder() {
  var name: Option[String] = None
  var fields: Option[List[TyField]] = None
  var derives: Option[List[String]] = None
  var attributes: Option[List[String]] = None
  var dataframe: Option[Boolean] = None
  var schema: Option[String] = None
  var comment: Option[String] = None
  def name(name: String): TyStructBuilder = {
    this.name = Some(name)
    this
  }
  def fields(fields: List[TyField]): TyStructBuilder = {
    this.fields = Some(fields)
    this
  }
  def derives(derives: List[String]): TyStructBuilder = {
    this.derives = Some(derives)
    this
  }
  def attributes(attributes: List[String]): TyStructBuilder = {
    this.attributes = Some(attributes)
    this
  }
  def dataframe(dataframe: Boolean): TyStructBuilder = {
    this.dataframe = Some(dataframe)
    this
  }
  def schema(schema: String): TyStructBuilder = {
    this.schema = Some(schema)
    this
  }
  def comment(comment: String): TyStructBuilder = {
    this.comment = Some(comment)
    this
  }
  def build(): TyStructImpl = {
    TyStructImpl(name, fields, derives, attributes, dataframe, schema, comment)
  }
}
class TyClassBuilder() {
  def build(): TyClassImpl = {
    TyClassImpl()
  }
}
class TyRecordBuilder() {
  def build(): TyRecordImpl = {
    TyRecordImpl()
  }
}
class TyFieldBuilder() {
  var name: Option[String] = None
  var value: Option[TyNode] = None
  var mutability: Option[Boolean] = None
  var defaultNone: Option[Boolean] = None
  def name(name: String): TyFieldBuilder = {
    this.name = Some(name)
    this
  }
  def value(value: TyNode): TyFieldBuilder = {
    this.value = Some(value)
    this
  }
  def mutability(mutability: Boolean): TyFieldBuilder = {
    this.mutability = Some(mutability)
    this
  }
  def defaultNone(defaultNone: Boolean): TyFieldBuilder = {
    this.defaultNone = Some(defaultNone)
    this
  }
  def build(): TyFieldImpl = {
    TyFieldImpl(name, value.get, mutability, defaultNone)
  }
}
class TyMapBuilder() {
  var key: Option[TyNode] = None
  var value: Option[TyNode] = None
  def key(key: TyNode): TyMapBuilder = {
    this.key = Some(key)
    this
  }
  def value(value: TyNode): TyMapBuilder = {
    this.value = Some(value)
    this
  }
  def build(): TyMapImpl = {
    TyMapImpl(key.get, value.get)
  }
}
class TyUuidBuilder() {
  def build(): TyUuidImpl = {
    TyUuidImpl()
  }
}
class TyResultBuilder() {
  var ok: Option[TyNode] = None
  var err: Option[TyNode] = None
  def ok(ok: TyNode): TyResultBuilder = {
    this.ok = Some(ok)
    this
  }
  def err(err: TyNode): TyResultBuilder = {
    this.err = Some(err)
    this
  }
  def build(): TyResultImpl = {
    TyResultImpl(ok.get, err.get)
  }
}
class TyCharBuilder() {
  def build(): TyCharImpl = {
    TyCharImpl()
  }
}
class TyNullBuilder() {
  def build(): TyNullImpl = {
    TyNullImpl()
  }
}
class TyNothingBuilder() {
  def build(): TyNothingImpl = {
    TyNothingImpl()
  }
}
class TyBooleanBuilder() {
  def build(): TyBooleanImpl = {
    TyBooleanImpl()
  }
}
class TyNumericBuilder() {
  def build(): TyNumericImpl = {
    TyNumericImpl()
  }
}
class TyIntegerBuilder() {
  var bitSize: Option[BitSize] = None
  var sized: Option[Boolean] = None
  def bitSize(bitSize: BitSize): TyIntegerBuilder = {
    this.bitSize = Some(bitSize)
    this
  }
  def sized(sized: Boolean): TyIntegerBuilder = {
    this.sized = Some(sized)
    this
  }
  def build(): TyIntegerImpl = {
    TyIntegerImpl(bitSize, sized)
  }
}
class TyUnitBuilder() {
  def build(): TyUnitImpl = {
    TyUnitImpl()
  }
}
class TyListBuilder() {
  var content: Option[TyNode] = None
  def content(content: TyNode): TyListBuilder = {
    this.content = Some(content)
    this
  }
  def build(): TyListImpl = {
    TyListImpl(content.get)
  }
}
class TyUndefinedBuilder() {
  def build(): TyUndefinedImpl = {
    TyUndefinedImpl()
  }
}