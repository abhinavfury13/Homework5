__Abhinav Ramnath__

<h1>Homework 5</h1>

<h2>Steps to compile, test and run:</h2>

1) Clone the repository and open in intellij or use the clone from version control option on intellij
2) Open Module Settings by right clicking the project name on the Project Toolbar and mark the folder src/main/scala as sources and src/test/scala as test 
3) Configure it to run with JDK version 18 and Scala version 3.1.3 using sbt 1.7
4) I have already provided the build.sbt along with the necessary changes to run scalatest. There will be a pop-up on the right to load sbt project(if sbt project does not load it automatically) please click on that to load the sbt settings. 
(or)
Modify the build.sbt file to include scalatest by adding the following lines to .settings: libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.13" % Test, libraryDependencies += "org.scalatest" %% "scalatest-featurespec" % "3.2.13" % Test,
and load the project with these new settings

<h2>Implementation Details</h2>
This is a language built to define classes, abstract classes, interfaces and showcase inheritance and overriding, exception handling and branching statements and partial evaluation.

I've implemented a model where the classes, interfaces and abstract classes are singleton (all objects of this class have the same reference)

<h2>Details on Partial Evaluation</h2>
Partial Evaluation is carried out by using a monadic function map and transformers that simplify Logic Gate Expressions <br>

<h3>map(Monadic Function)</h3>
It is the function that applies the transformers on a given partial expression <br>

<strong>Definition</strong> - def map(expr: LogicGates, f: LogicGates => LogicGates| Boolean): LogicGates| Boolean <br>
<strong>Usage</strong> - map(AND(OR(true,false),true)), simplifyAND) //here simplifyAND is a transformer function that optimizes the AND Gate<br>

<h3> Transformer Functions(Optimizers) </h3>
They are all defined as function literals with the syntax LogicGates => LogicGates|Boolean but vary according to the gate that they optimize <br>

There are 4 transformer functions: simplifyAND, simplifyOR, simplifyNAND, simplifyNOR <br>

I shall show simplifyAND here as an example: <br>
  private val simplifyAND: LogicGates => LogicGates | Boolean = <br>
    (expression: LogicGates) => expression match <br>
      case AND(false, _) => false <br>
      case AND(_, false) => false <br>
      case AND(true, o1) => o1 <br>
      case AND(o1, true) => o1 <br>
      case AND(o1, o2) => AND(o1, o2) <br>
      
//Here if any of the values are false => it evaluates to false,<br> if any argument is true => the value is the evaluation of the 2nd argument <br> if both are not defined it returns it as it is      



<h2>IF</h2>
This models the if statement that is present in all languages <br>
It uses call by name parameters for all arguments <br>
<strong>Definition</strong> - IF(condition:=> Boolean, thenBlock :=> Unit, elseBlock :=>Unit) <br>
<strong>Usage</strong> - IF({AND(true,false).eval},{//statements in then block},{//statement in else block}) <br>
<em><strong>Note:We can have any number of nested if's</em></strong> <br><br>

<h2>CatchException</h2>
This is used to define the try catch blocks in the code <br>
We use a stack to know if a current exception has occurred and evaluate statements based on the value in the stack <br>
It uses call by name parameters for tryBlock and catchBlock arguments <br>
<strong>Definition</strong> - CatchException(exceptionClassName: String, tryBlock :=> Unit, catchBlock :=>Unit) <br>
<strong>Usage</strong> - CatchException("exceptionName",{//try block statements},{//catch block statements(will only get evaluated if exception is thrown)}) <br>
<em><strong>Note:We can have any number of nested CatchException blocks, and exceptions thrown in inner blocks can be caught in outer blocks </em></strong> <br><br>

<h2>Scope</h2>
This is a modified version of scope which takes a call by name parameter, it binds a specific block of statements to a scope
It uses call by name parameters for block argument
<strong>Definition</strong> - Scope(scopeName: String, block :=> Unit) <br>
<strong>Usage</strong> - Scope("scopeName",{//statements inside that scope}) <br>
<em><strong>Note:We can have any number of nested scope's(definition in outer scopes will be visible in inner scopes)</em></strong> <br><br>

<h2>ClassBuilder</h2>
This is an enum that contains all the definitions for building classes and inheritance <br>
It contains - ThrowException, ExceptionClassDef, ClassDef, AbstractClassDef, InterfaceDef, Constructo, Implements, InvokeMethod, Method, AbstractMethod, Field, Extends, Protected, Private and New Object

<h2>ExceptionClassDef</h2>
 This is used to define an exception class.
 
 <strong>Definition</strong> - ExceptionClassDef(exceptionClassName: String)<br>
 <strong>Usage</strong> - ExceptionClassDef("MyException") <br>
 We store the exceptions in an exceptionClassMap along with reason<br>
 exceptionClassMap = exceptionClassName -> reason (Eg. "RunTimeException"-> "Divide by Zero") <br> 
 
<h2>ThrowException</h2>
 This is used throw an exception to stop the linear flow of control
 
 <strong>Definition</strong> - ThrowException(exceptionClassName :String, reason: String)<br>
 <strong>Usage</strong> - ThrowException("RunTimeException", "Divide by Zero") <br>
 We store the exceptions in an exceptionClassMap along with reason<br>
 exceptionClassMap = exceptionClassName -> reason (Eg. "RunTimeException"-> "Divide by Zero") <br> 
 
 <h2>Implements</h2>
 This is used to specify the interfaces that are implemented by class. We specify this during ClassDef or AbstractClassDef
 <em><strong>Note: Interfaces can extend another interface but cannot implement another interface</em></strong><br>
 
 <strong>Definition</strong> - Implements(interfaces :String*) (var args since we can implement multiple interfaces)<br>
 <strong>Usage</strong> - AbstractClassDef("classA", Implements("interfaceB", "interface C")) <br>
 We store the implements relations in an implementsMap<br>
 implementsMap: String->Set[String] Eg. classA -> (interfaceB, interfaceC) <br> 

<h2>ClassDef</h2>
This is defined as a case that takes 2 paramaters, the className and var args(or Seq) of type ClassBuilder <br>
It can only define concrete methods <br>

  <strong>Definition</strong> - ClassDef(className: String, ClassBuilder*) <br>
  <strong>Usage</strong> - ClassDef("class1", Private(Field("A")), Field("B"), Protected(Field("C")), Constructor(Assign("B",true)).eval <br>
  <em><strong>Note: If we enclose method or field declaration inside Private() or Protected() they are set to those specifiers, else the default access specifier is public</em></strong> <br><br>
  It is stored in a classMap that contains access Maps, one for each access specifier <br>
  Each access specifier holds 2 maps that map the fieldMap and methodMap <br>
 <h2>Class Map Structure</h2>
  classMap: String -> (accessMap) <br> Eg. "classA" -> (accessMap) <br>
  accessmap: String -> (fieldMap, methodMap) Eg. "Public" -> (fieldMap, methodMap) <br>
  fieldMap: String -> Boolean Eg. A -> true <br>
  <em>//maps function name and number of parameters -> Function definition and parameter names</em> <br>
  methodMap: (String,Int) -> (LogicGates|Boolean, Seq[String])  Eg. ("method1",2) -> (AND(Input("A"), Input("B")), Seq("A", "B")) <br>
  
<h2>AbstractClassDef</h2>
This is defined as a case that takes 2 paramaters, the className and var args(or Seq) of type ClassBuilder <br>
It can have both abstract methods and methods <br>

  <strong>Definition</strong> - AbstractClassDef(className: String, ClassBuilder*) <br>
  <strong>Usage</strong> - AbstractClassDef("class1", Private(Field("A")), Field("B"), Protected(Field("C")), Constructor(Assign("B",true)).eval <br>
  <em><strong>Note: If we enclose method or field declaration inside Private() or Protected() they are set to those specifiers, else the default access specifier is public</em></strong> <br><br>
  It is stored in a abstractClassMap that contains access Maps, one for each access specifier <br>
  Each access specifier holds 3 maps that map the fieldMap, methodMap and abstractMethodList<br>
 <h2>Abstract Class Map Structure</h2>
  classMap: String -> (accessMap) <br> Eg. "classA" -> (accessMap) <br>
  accessmap: String -> (fieldMap, methodMap, abstractMethodList) Eg. "Public" -> (fieldMap, methodMap, abstractMethodList) <br>
  fieldMap: String -> Boolean Eg. A -> true <br>
  <em>//maps function name and number of parameters -> Function definition and parameter names</em> <br>
  methodMap: (String,Int) -> (LogicGates|Boolean, Seq[String])  Eg. ("method1",2) -> (AND(Input("A"), Input("B")), Seq("A", "B")) <br>
  <em>//Contains a Set of methodNames with parameters</em> <br>
  abstractMethodList: Set(String,Int) Eg. Seq(("method1",0), ("method2",2))
  
<h2>InterfaceDef</h2>
This is defined as a case that takes 2 paramaters, the className and var args(or Seq) of type ClassBuilder <br>
It can have only abstract methods. Cannot have a constructor <br>

  <strong>Definition</strong> - ClassDef(className: String, ClassBuilder*) <br>
  <strong>Usage</strong> - InterfaceDef("interface1", Private(Field("A")), Field("B"), Protected(Field("C"))).eval <br>
  <em><strong>Note: If we enclose method or field declaration inside Private() or Protected() they are set to those specifiers, else the default access specifier is public</em></strong> <br><br>
  It is stored in a interfaceMap that contains access Maps, one for each access specifier <br>
  Each access specifier holds 2 maps that map the fieldMap and abstractMethodList<br>
 <h2>Interface Map Structure</h2>
  interfaceMap: String -> (accessMap) <br> Eg. "classA" -> (accessMap) <br>
  accessmap: String -> (fieldMap, abstractMethodList) Eg. "Public" -> (fieldMap, abstractMethodList) <br>
  fieldMap: String -> Boolean Eg. A -> true <br>
  <em>//Contains a Set of methodNames with parameters</em> <br>
  abstractMethodList: Set(String,Int) Eg. Seq(("method1",0), ("method2",2))  
  
  <h2>Constructor</h2>
  This is contains a var args(or Seq) of Assign Statements that is initialized with the class
 
 <strong>Definition</strong> - Constructor(arguments: Assign*)
 <strong>Usage</strong> - Constructor(Assign("A",true), Assign("B", true)
 
 <h2>Extends</h2>
 This is used to specify the parent class of a current class. We specify this during ClassDef
 <em><strong>Note: Interfaces can extend another interface</em></strong><br>
 
 <strong>Definition</strong> - Extends(parentClassName:String) <br>
 <strong>Usage</strong> - AbstractClassDef("classA", Extends("classB")) // this inherits all the public and protected fields and methods from classB <br>
 <em><strong>Note: Multiple inheritance is not allowed</em></strong><br>
 We store the inheritance relations in an inheritanceMap<br>
 inheritanceMap: String->String Eg. classA -> classB //classB is the parent class of classA (or) interfaceA -> interfaceB <br> 
 
 <h2>Implements</h2>
 This is used to specify the interfaces that are implemented by class. We specify this during ClassDef or AbstractClassDef
 <em><strong>Note: Interfaces can extend another interface but cannot implement another interface</em></strong><br>
 
 <strong>Definition</strong> - Implements(interfaces :String*) (var args since we can implement multiple interfaces)<br>
 <strong>Usage</strong> - AbstractClassDef("classA", Implements("interfaceB", "interface C")) <br>
 We store the implements relations in an implementsMap<br>
 implementsMap: String->Set[String] Eg. classA -> (interfaceB, interfaceC) <br> 
 
 <h2>InvokeMethod</h2>
 This is used to invoke functions of a class, it invokes the functions of the current class and all function that are inherited and overridden
 
 <strong>Definition</strong> - InvokeMethod(objectName: String, methodName:String, Boolean*) //takes a variable number of arguments of type Boolean as parameters <br>
 <strong>Usage</strong> - InvokeMethod("class1", "method1", true, false) (or) InvokeMethod("class1", "Method2") <br>
 <em><strong>Note: To find the method we search through the maps of the current class as well as the maps of the classes it inherits from recursively</em></strong><br>
 
 <h2>Field</h2>
 This is used to define a field for the class. We specify this during ClassDef <br>
 <strong>Definition</strong> - Field(fieldName: String) <br>
 <strong>Usage</strong> - ClassDef("classA", Field("A")) <br>
 <em><strong>Note: All declared fields are initialized to default value false since they are of type boolean</em></strong><br>
 Stored using a fieldMap whose structure is shown above.
 
 <h2>Method</h2>
 This is used to define a method for the class. It may accept arbitrary number of parameters or have 0 parameters. We specify this during ClassDef  <br>
 <strong>Definition</strong> - Method(methodName:String, method: LogicGates|Boolean, parameters: String*) //takes variable number of parameters <br>
 <strong>Usage</strong> - ClassDef("classA", Method("m1", NOT(false)) (or) ClassDef("classA", Method("m1", NOT(Input("A"), "A")  <br>
 <em><strong>Note: If we are passing parameter we should declare as Input("A") and if we are using a class field we should declare as ClassField("A") </em></strong><br>
                          
 Stored using a methodMap whose structure is shown above.
 
 <h2>AbstractMethod</h2>
 This is used to define an abstract method. It may accept arbitrary number of parameters or have 0 parameters. We specify this during AbstractClassDef or InterfaceDef  <br>
 <strong>Definition</strong> - AbstractMethod(methodName:String, parameters: String*) //takes variable number of parameters <br>
 <strong>Usage</strong> - InterfaceDef("interfaceA", AbstractMethod("m1") (or) AbstractClassDef("classA", Method("m1", "A")  <br>
 <em><strong>Note: If we are passing parameter we should declare as Input("A") and if we are using a class field we should declare as ClassField("A") </em></strong><br>
                          
 Stored using a abstractMethodList whose structure is shown above.
 
 <h2>NewObject</h2>
 This is used to create an object of a class <br>
 We check if all abstract methods are overriden and call the constructors in the order of execution here <br>
  <strong>Definition</strong> - NewObject(className:String, objectName:String) <br>
  <strong>Usage</strong> - NewObject("classA", "objectA") //creates objectA of type classA  <br>
  <em><strong>Note: Classes are singleton so all object share the same reference</em></strong><br>
  
  Stored in an objectMap <br>
  objectMap: String->String Eg. objectA -> classA <br>
  
<h2> Limitations </h2>
Language does not allow nested class, interface or abstract class definition definition <br>
All classes, interfaces and abstract classes are singleton (multiple unique objects of the class cannot be created)
Default methods have not been implemented for interfaces
