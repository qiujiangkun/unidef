from unidef.languages.common.type_model import DyType
from unidef.models.base_model import FieldValue, MixedModel
from unidef.models.typed_field import TypedField
from unidef.utils.safelist import safelist
from unidef.utils.typing_ext import *


class Attribute(TypedField):
    pass


class Attributes:
    Kind = Attribute(key="kind", ty=str)
    Name = Attribute(key="name", ty=str)
    VariableDeclarationId = Attribute(key="variable_declaration_id", ty=Union[str, Any])
    Children = Attribute(key="children", ty=list)
    Statement = Attribute(key="statement", ty=Any)
    ClassDeclaration = Attribute(key="class_declaration", ty=bool)
    Fields = Attribute(key="fields", ty=List[DyType])
    SuperClasses = Attribute(key="super_class", ty=list)
    Functions = Attribute(key="functions", ty=list)

    WhileLoop = Attribute(key="while_loop", ty=Any)

    CForLoop = Attribute(key="c_for_loop", ty=bool)
    CForLoopInit = Attribute(key="c_for_loop_init", ty=Any)
    CForLoopTest = Attribute(key="c_for_loop_test", ty=Any)
    CForLoopUpdate = Attribute(key="c_for_loop_update", ty=Any)

    Expression = Attribute(key="expression", ty=Any)
    FunctionCall = Attribute(key="function_call", ty=bool)
    FunctionDecl = Attribute(key="function_decl", ty=bool)
    FunctionBody = Attribute(key="function_body", ty=Any)
    FunctionReturn = Attribute(key="function_return", ty=Any)
    RawCode = Attribute(key="raw_code", ty=str)
    RawValue = Attribute(key="raw_value", ty=Any)
    Callee = Attribute(key="callee", ty=Any)
    Arguments = Attribute(key="arguments", ty=list)
    Argument = Attribute(key="argument", ty=bool)
    ArgumentName = Attribute(key="argument_name", ty=Any)
    ArgumentType = Attribute(key="argument_type", ty=Any)
    DefaultValue = Attribute(key="default_value", ty=Any)
    Literal = Attribute(key="literal", ty=bool)
    Async = Attribute(key="async", ty=bool, default=False)
    Return = Attribute(key="return", ty=Optional)

    VariableDeclarations = Attribute(key="variable_declarations", ty=list)
    VariableDeclaration = Attribute(key="variable_declaration", ty=bool)

    Print = Attribute(key="print", ty=bool)
    Requires = Attribute(key="requires", ty=list)
    Require = Attribute(key="require", ty=list)
    RequirePath = Attribute(key="require_path", ty=str)
    RequireKey = Attribute(key="require_key", ty=str)
    RequireValue = Attribute(key="require_value", ty=str)
    ObjectProperties = Attribute(key="object_properties", ty=list)
    ObjectProperty = Attribute(key="object_property", ty=bool)
    KeyName = Attribute(key="key", ty=Any)
    Value = Attribute(key="value", ty=Any)
    ArrayElements = Attribute(key="array_elements", ty=list)

    TestExpression = Attribute(key="test_expression", ty=Any)
    IfClauses = Attribute(key="if_clauses", ty=bool)
    IfClause = Attribute(key="if_clause", ty=bool)
    ElseIfClause = Attribute(key="else_if_clause", ty=bool)
    ElseClause = Attribute(key="else_clause", ty=bool)

    BlockStatement = Attribute(key="block_statement", ty=bool)
    Consequence = Attribute(key="consequence", ty=Any)
    Alternative = Attribute(key="alternative", ty=Any)
    Operator = Attribute(key="operator", ty=str)
    OperatorLeft = Attribute(key="operator_left", ty=Any)
    OperatorMiddle = Attribute(key="operator_middle", ty=Any)
    OperatorRight = Attribute(key="operator_right", ty=Any)
    OperatorSinglePrefix = Attribute(key="operator_single_prefix", ty=Any)
    OperatorSinglePostfix = Attribute(key="operator_single_postfix", ty=Any)

    StaticMemberExpression = Attribute(key="static_member_expression", ty=bool)
    ComputedMemberExpression = Attribute(key="computed_member_expression", ty=bool)
    MemberExpressionObject = Attribute(key="member_expression_object", ty=Any)
    MemberExpressionProperty = Attribute(key="member_expression_property", ty=Any)

    Identifier = Attribute(key="identifier", ty=str)
    ThisExpression = Attribute(key="this_expression", ty=bool)
    SuperExpression = Attribute(key="super_expression", ty=bool)

    Directive = Attribute(key="directive", ty=Any)

    Program = Attribute(key="program", ty=bool)

    AssignExpression = Attribute(key="assign_expression", ty=bool)
    AssignExpressionLeft = Attribute(key="assign_expression_left", ty=Any)
    AssignExpressionRight = Attribute(key="assign_expression_right", ty=Any)

    AwaitExpression = Attribute(key="await_expression", ty=Any)
    ThrowStatement = Attribute(key="throw_statement", ty=Any)
    NewExpression = Attribute(key="new_expression", ty=bool)
    ConditionalExpression = Attribute(key="conditional_expression", ty=bool)
    BreakStatement = Attribute(key="break_statement", ty=bool)
    ContinueStatement = Attribute(key="continue_statement", ty=bool)

    TryStatement = Attribute(key="try_statement", ty=list)
    CatchClauses = Attribute(key="catch_clauses", ty=list)
    CatchClause = Attribute(key="catch_clause", ty=bool)
    FinallyClause = Attribute(key="finally_statement", ty=list)

    Mutable = Attribute(key="mutable", ty=bool)

    InferredType = Attribute(key="inferred_type", ty=Any)

    GlobalPath = Attribute(key="global_path", ty=str)


class IrNode(MixedModel):
    @classmethod
    @beartype
    def from_str(cls, name: str) -> __qualname__:
        return cls().append_field(Attributes.Kind(name))

    @classmethod
    @beartype
    def from_attribute(cls, attr: FieldValue) -> __qualname__:
        this = cls.from_str(attr.key).append_field(attr)
        return this


class ParameterNode(IrNode):
    kind: str = "argument"
    argument_name: str
    argument_type: Optional[DyType]
    default: Optional[IrNode]
    # Attributes.Mutable
    mutable: Optional[bool] = None
    # used in SQL
    output: Optional[bool] = None


class AssignmentExpressionNode(IrNode):
    kind: str = 'assignment_expression'
    assignee: IrNode
    value: IrNode


class StatementNode(IrNode):
    value: IrNode


class Children(IrNode):
    kind: str = "children"
    children: List[IrNode]

    def __init__(self, children):
        self.children = children

class BlockStatementNode(IrNode):
    children: List[IrNode]


class ThisExpressionNode(IrNode):
    this: str


class SuperExpressionNode(IrNode):
    super: str


class IfExpressionNode(IrNode):
    test: IrNode
    consequent: Optional[IrNode]
    alternative: Optional[IrNode]
    conditional_op: bool = False


class BreakStatementNode(IrNode):
    content: Optional[IrNode] = None
    target: str = ''


class ContinueStatementNode(IrNode):
    target: str = ''


class CatchClauseNode(IrNode):
    argument_name: IrNode
    var_id: Optional[str] = None
    body: Children

class TryStatementNode(IrNode):
    try_body: List[IrNode]
    catch_clauses: List[CatchClauseNode]
    finally_clause: Optional[IrNode] = None


# let {a, b} = c;
class DecomposePatternNode(IrNode):
    names: List[str]
    object: bool = True

class VariableDeclarationNode(IrNode):
    id: Union[str, DecomposePatternNode]
    ty: Optional[DyType]
    init: Optional[IrNode]
    mutable: Optional[bool] = None

class VariableDeclarationsNode(IrNode):
    decls: List[VariableDeclarationNode]


class FunctionDecl(IrNode):
    kind: str = "function_decl"
    name: str
    accessibility: Optional[str] = None
    arguments: List[ParameterNode]
    function_return: Optional[DyType] = None
    function_body: Optional[Children] = None
    is_async: bool = False


class FunctionCallNode(IrNode):
    kind: str = "function_call"
    callee: IrNode
    arguments: List[IrNode]


class ClassDeclNode(IrNode):
    kind: str = "class_declaration"
    name: str
    super_class: List[str] = []
    fields: List[DyType] = []
    functions: List[FunctionDecl]


class ProgramNode(IrNode):
    body: Children


class RawCodeNode(IrNode):
    code: Any


class MemberExpressionNode(IrNode):
    static: bool
    obj: IrNode
    property: IrNode


class LiteralNode(IrNode):
    raw_value: Any
    raw_code: str


class IdentifierNode(IrNode):
    identifier: str


class DirectiveNode(IrNode):
    directive: str

class ReturnNode(IrNode):
    returnee: Optional[IrNode]

class OperatorNode(IrNode):
    operator: str
    kind: str  # prefix, postfix, binary, trinary
    left: Optional[IrNode] = None
    right: Optional[IrNode] = None
    value: Optional[IrNode] = None


class AwaitExpressionNode(IrNode):
    value: IrNode


class ThrowStatementNode(IrNode):
    content: IrNode


class NewExpressionNode(IrNode):
    type: IrNode
    arguments: List[IrNode]


class ClassicalLoopNode(IrNode):
    init: VariableDeclarationsNode
    test: IrNode
    update: IrNode
    body: IrNode

class RequireNode(IrNode):
    path: str
    key: str
    value: str

class RequiresNode(IrNode):
    requires: List[RequireNode]

    @staticmethod
    def requires(import_paths, import_name) -> __qualname__:
        if isinstance(import_name, list):
            if isinstance(import_paths, list):
                requires_result = [
                    RequireNode(path=path, key=kv.get(0), value=kv.get(1))
                    for kv, path in zip(map(safelist, import_name), import_paths)
                ]
            else:
                requires_result = [
                    RequireNode(path=import_paths, key=kv.get(0), value=kv.get(1))
                    for kv in map(safelist, import_name)
                ]
        elif isinstance(import_name, tuple):
            requires_result = [
                RequireNode(
                    path=import_paths, key=import_name[0], value=import_name[1]
                )
            ]
        else:
            requires_result = [RequireNode(path=import_paths, key=import_name, value='')]
        return RequiresNode(requires=requires_result)
class Nodes:
    @staticmethod
    def print(content: IrNode) -> IrNode:
        return IrNode.from_attribute(Attributes.Print(True)).append_field(
            Attributes.Children(content)
        )

