import CustomClassBuilder.LogicGates.*
import CustomClassBuilder.ClassBuilder.Assign
import CustomClassBuilder.ClassBuilder.Field

import scala.annotation.tailrec
import scala.runtime.Nothing$
object CustomClassBuilder:

  //Type definition for a map that maps the fields of a class fieldName -> BooleanValue
  type fieldMapType = collection.mutable.Map[String, Boolean]

  //Type definition for a map that maps the method and number of parameters to an expression with it's parameters (methodName, number_of_parameters) -> LogicGates, Seq[Boolean]
  type methodMapType = collection.mutable.Map[(String, Int), (LogicGates | Boolean, Seq[String])]

  //Type definition for a Set that maps the abstract fields of a abstractclass or interface
  type abstractMethodListType = collection.mutable.Set[(String, Int)]

  //Type definition for a map that contains maps to the fields and methods of each access specifier accessSpecifierName -> (fieldMap, methodMap)
  type classAccessMapType = collection.mutable.Map[String, (fieldMapType, methodMapType)]

  //Type definition for a map that contains maps to the fields, methods and abstractMethods for each access specifier accessSpecifierName -> (fieldMap, methodMap, abstractMethodList)
  type abstractClassAccessMapType = collection.mutable.Map[String, (fieldMapType, methodMapType, abstractMethodListType)]

  //Type definition for a map that contains maps to the fields and abstractMethods for an interface for each access specifier accessSpecifierName -> (fieldMap, abstractMethodList)
  type interfaceAccessMapType = collection.mutable.Map[String, (fieldMapType, abstractMethodListType)]

  //Map that contains the exception classes
  private val exceptionClassMap: collection.mutable.Map[String, String] = collection.mutable.Map()

  // A mutable map for each access specifier in class(Public, Protected, Private) Eg. Public -> (fieldMap, methodMap)
  private val classAccessMap: classAccessMapType = collection.mutable.Map()

  // A mutable map for each access specifier in abstract class(Public, Protected, Private) Eg. Public -> (fieldMap, methodMap, abstractMethodList)
  private val abstractClassAccessMap: abstractClassAccessMapType = collection.mutable.Map()

  // A mutable map for each access specifier in interface(Public, Protected, Private) Eg. Public -> (fieldMap, abstractMethodList)
  private val interfaceAccessMap: interfaceAccessMapType = collection.mutable.Map()

  //A mutable map that contains the access Specifier Map Eg. className ->(Public -> (fieldMap, methodMap), Protected -> (fieldMap, methodMap), Private -> (fieldMap, methodMap))
  private val classMap: collection.mutable.Map[String, classAccessMapType] = collection.mutable.Map()

  //A mutable map that contains the access Specifier Map Eg. abstractClassName -> (Public -> (fieldMap, methodMap, abstractMethodList), Protected -> (fieldMap, methodMap, abstractMethodList), Private -> (fieldMap, methodMap, abstractMethodList))
  private val abstractClassMap: collection.mutable.Map[String, abstractClassAccessMapType] = collection.mutable.Map()

  //A mutable map that contains the access Specifier Map Eg. className ->(Public -> (fieldMap, abstractMethodList), Protected -> (fieldMap, abstractMethodList), Private -> (fieldMap, abstractMethodList))
  private val interfaceMap: collection.mutable.Map[String, interfaceAccessMapType] = collection.mutable.Map()

  //A mutable map that maps fieldName -> BooleanValue
  private val fieldMap: fieldMapType = collection.mutable.Map()

  //A mutable Set that contains the abstract methods, number of parameters
  private val abstractMethodList: abstractMethodListType = collection.mutable.Set()

  //A mutable map that maps the method and number of parameters to an expression (methodName, number_of_parameters) -> LogicGates
  private val methodMap: methodMapType = collection.mutable.Map()

  //A stack that contains the current Scope of Execution(Name of Class, Interface or Abstract Class whose objects are being evaluated)
  private val currentscopeStack: collection.mutable.Stack[String] = collection.mutable.Stack()

  //A stack that contains the  Scope of Execution
  private val scopeStack: collection.mutable.Stack[String] = collection.mutable.Stack()

  //A stack that contains the current AccessModifier for which fields and methods are being defined
  private val accessModifierStack: collection.mutable.Stack[String] = collection.mutable.Stack()

  //A stack that contains the current type(Interface, Class, Abstract Class) for which fields and methods are being defined
  private val typeStack: collection.mutable.Stack[String] = collection.mutable.Stack()

  //A mutable map that maps the class hierarchy Eg. If classA inherits classB classA -> classB
  private val inheritanceMap: collection.mutable.Map[String, String] = collection.mutable.Map()

  //A mutable map that maps the implements hierarchy Eg. If classA implements interfaceB,interfaceC classA -> Set(interfaceB, interfaceC)
  private val implementsList: collection.mutable.Map[String, collection.mutable.Set[String]] = collection.mutable.Map()

  //Since we use singleton objects, there is only object per class, we just map the objectName -> className
  private val objectMap: collection.mutable.Map[String, String] = collection.mutable.Map()

  //Map used to maintain the parameters of the current function being invoked
  private val parameterMap: collection.mutable.Map[String, Boolean] = collection.mutable.Map()

  //Map used to map the className to the constructor of the class, constructor is Invoked when an object of the class is invoked.
  private val constructorMap: collection.mutable.Map[String, Seq[Assign]] = collection.mutable.Map()

  //Stack used to store the sequence of assigns to be executed in order Parent-> Child1-> Child2 and so on
  private val constructorStack: collection.mutable.Stack[Seq[Assign]] = collection.mutable.Stack()

  //Mutable Set that contains all the methods that need to be overridden for this class
  private val overrideMethodList: collection.mutable.Set[(String,Int)] = collection.mutable.Set()

  //Stack used to check if a exception has been thrown (exceptionClassName, reason)
  private val exceptionStack: collection.mutable.Stack[String] = collection.mutable.Stack()

  //Map used to store the values that are inserted using insert, maps (scopeName, variableName) -> Value
  private val scopeMap: collection.mutable.Map[(String,String),Boolean] = collection.mutable.Map()

  //Transformer function to simplify AND LogicGate
  private val simplifyAND: LogicGates => LogicGates | Boolean =
    (expression: LogicGates) => expression match
      case AND(false, _) => false
      case AND(_, false) => false
      case AND(true, o1) => o1
      case AND(o1, true) => o1
      case AND(o1, o2) => AND(o1, o2)

  //Transformer function to simplify OR LogicGate
  private val simplifyOR: LogicGates => LogicGates | Boolean =
    (expression: LogicGates) => expression match
      case OR(true, _) => true
      case OR(_, true) => true
      case OR(false, o1) => o1
      case OR(o1, false) => o1
      case OR(o1, o2) => OR(o1, o2)

  //Transformer function to simplify NAND LogicGate
  private val simplifyNAND: LogicGates => LogicGates | Boolean =
    (expression: LogicGates) => expression match
      case NAND(false, _) => true
      case NAND(_, false) => true
      case NAND(o1, o2) => NAND(o1, o2)

  //Transformer function to simplify NOR LogicGate
  private val simplifyNOR: LogicGates => LogicGates | Boolean =
    (expression: LogicGates) => expression match
      case NOR(true, _) => false
      case NOR(_, true) => false
      case NOR(o1, o2) => NOR(o1, o2)

  //Find type if it's a class, abstract class or interface
  //Returns Option[String], i.e Some("Class") if it's a class and so on, and None otherwise
  def FindType(name:String) : Option[String] =
    if classMap.contains(name) then
      Some("Class")
    else if abstractClassMap.contains(name) then
      Some("Abstract Class")
    else if interfaceMap.contains(name) then
      Some("Interface")
    else
      None

  //Used to find if the given string belong to a class or abstract class
  //Return true if class or abstract class, false otherwise
  def FindClass(classname:String): Boolean =
    if classMap.contains(classname) then
      true
    else if abstractClassMap.contains(classname) then
      true
    else
      false

  //Function Used to check the existing class Map if the method exists in a particular accessModifier(Also used to detect duplicate method declaration)
  //Return: true if method found in accessModifier, false otherwise
  def checkClassMapForMethod(className: String, accessModifier: String, methodName: String, parameters: Int): Boolean =
    classMap(className).get(accessModifier) match
      case None => false
      case Some(o1) =>
        if o1._2.contains(methodName, parameters) then true else false

  //Function Used to check the existing abstract class Map if the method exists in a particular accessModifier(Also used to detect duplicate method declaration)
  //Return: true if method found in accessModifier, false otherwise
  def checkAbstractClassMapForMethod(className: String, accessModifier: String, methodName: String, parameters: Int): Boolean =
    abstractClassMap(className).get(accessModifier) match
        case None => false
        case Some(o1) =>
          if o1._2.contains(methodName, parameters) then true else false

  //Function Used to check the existing abstract class Map if the abstract method exists in a particular accessModifier(Also used to detect duplicate method declaration)
  //Return: true if abstractmethodlist found in accessModifier, false otherwise
  def checkAbstractClassMapForAbstractMethodList(className: String, accessModifier: String): Boolean =
    abstractClassMap.get(className) match
      case None => false
      case Some(mapObject) => mapObject.get(accessModifier) match
        case None => false
        case Some(o1) =>
          //If abstractMethodList is empty return false
          if o1._3.isEmpty then
            false
          else
            true

  //Function Used to check the existing interface Map if the abstract method exists in a particular accessModifier(Also used to detect duplicate method declaration)
  //Return: true if abstractmethodlist found in accessModifier, false otherwise
  def checkInterfaceForAbstractMethodList(interfaceName: String, accessModifier: String): Boolean =
    interfaceMap.get(interfaceName) match
      case None => false
      case Some(mapObject) => mapObject.get(accessModifier) match
        case None => false
        case Some(o1) =>
          //If abstractMethodList is empty return false
          if o1._2.isEmpty then
            false
          else
            true

  //Function Used to find method in the class(It checks through all the access modifiers. Also used to detect duplicate method declaration)
  //Returns: Some[(LogicGates|Boolean, Seq[String])] i.e method and it's params if method exists and None if it does not
  def findMethodInClass(className: String, methodName: String, parameters: Int): Option[(LogicGates | Boolean, Seq[String])] =
    if checkClassMapForMethod(className, "Public", methodName, parameters) then
      classMap(className)("Public")._2.get(methodName, parameters) match
        case Some(method, params) => Some(method, params)
    else if checkClassMapForMethod(className, "Protected", methodName, parameters) then
      classMap(className)("Protected")._2.get(methodName, parameters) match
        case Some(method, params) => Some(method, params)
    else
      None

  //Function Used to find method in the abstract class(It checks through all the access modifiers. Also used to detect duplicate method declaration)
  //Returns: Some[(LogicGates|Boolean, Seq[String])] i.e method and it's params if method exists and None if it does not
  def findMethodInAbstractClass(className:String, methodName: String, parameters: Int): Option[(LogicGates | Boolean, Seq[String])] =
    if checkAbstractClassMapForMethod(className, "Public", methodName, parameters) then
      abstractClassMap(className)("Public")._2.get(methodName, parameters) match
        case Some(method, params) => Some(method, params)
    else if checkAbstractClassMapForMethod(className, "Protected", methodName, parameters) then
      abstractClassMap(className)("Protected")._2.get(methodName, parameters) match
        case Some(method, params) => Some(method, params)
    else
      None

  //Function Used to find method in the abstract class or concrete class(It checks through all the access modifiers. Also used to detect duplicate method declaration)
  //Returns: Some[(LogicGates|Boolean, Seq[String])] i.e method and it's params if method exists and None if it does not
  def findMethod(className: String, methodName: String, parameters: Int): Option[(LogicGates | Boolean, Seq[String])] =
    if FindType(className).contains("Class") then
      findMethodInClass(className, methodName, parameters)
    else if FindType(className).contains("Abstract Class") then
      findMethodInAbstractClass(className, methodName, parameters)
    else
      None

  //Function Used to find abstract method in the abstract class(It checks through all the access modifiers. Also used to detect duplicate method declaration)
  //Returns: Some[Set[(String, Int)]] i.e set of methods and it's params if methods exists and None if it does not
  def getAbstractMethodsInAbstractClass(className: String): Option[abstractMethodListType] =
    val returnSet: abstractMethodListType = collection.mutable.Set()
    if checkAbstractClassMapForAbstractMethodList(className, "Public") then
      returnSet.addAll(abstractClassMap(className)("Public")._3)
    if checkAbstractClassMapForAbstractMethodList(className, "Protected") then
      returnSet.addAll(abstractClassMap(className)("Protected")._3)
    if returnSet.isEmpty then
      None
    else
      Some(returnSet)

  //Function Used to find abstract method in the abstract class(It checks through all the access modifiers. Also used to detect duplicate method declaration)
  //Returns: Some[Set[(String, Int)]] i.e set of methods and it's params if methods exists and None if it does not
  def getAbstractMethodsInInterface(className: String): Option[abstractMethodListType] =
    val returnSet: abstractMethodListType = collection.mutable.Set()
    if checkInterfaceForAbstractMethodList(className, "Public") then
      returnSet.addAll(interfaceMap(className)("Public")._2)
    if checkInterfaceForAbstractMethodList(className,"Protected") then
      returnSet.addAll(interfaceMap(className)("Protected")._2)
    if returnSet.isEmpty then
      None
    else
      Some(returnSet)

  //Function used to recursive check superClasses and Interfaces for methods
  def recursivelyCheckSuperClassorInterface(name: String): Unit =
    inheritanceMap.get(name) match
      case Some(superClassName) =>
        if superClassName != "None" then
          retrieveAbstractMethodListFromInheritance(superClassName)
      case None =>

  def retrieveAbstractMethodListFromInheritance(name:String): Unit =
    if FindType(name).contains("Class") then
      //Recursively check upper classes
      recursivelyCheckSuperClassorInterface(name)
    else if FindType(name).contains("Abstract Class") then
      getAbstractMethodsInAbstractClass(name) match
        case Some(abstractMethodSet) =>
          overrideMethodList.addAll(abstractMethodSet)
        case None =>
      //Recursively check upper classes
      recursivelyCheckSuperClassorInterface(name)
      implementsList.get(name) match
        case  Some(interfaceSet) =>
          for(interfaceName <- interfaceSet)
            retrieveAbstractMethodListFromInheritance(interfaceName)
        case None =>
    else if FindType(name).contains("Interface") then
      getAbstractMethodsInInterface(name) match
        case Some(abstractmethodSet) =>
          overrideMethodList.addAll(abstractmethodSet)
        case None =>
      //Recursively check upper interfaces
      recursivelyCheckSuperClassorInterface(name)

  //Function used to check if all the abstract methods that we inherit or implement from have been overridden
  //Returns true if yes, throws an exception if not
  def CheckOverriding(className: String): Boolean =
    retrieveAbstractMethodListFromInheritance(className)
    if overrideMethodList.nonEmpty then
      for((methodName, parameters) <- overrideMethodList)
        findMethodFromInheritance(className, methodName, parameters) match
          case Some(_,_) =>
          case None => throw new Exception("Abstract methods not overridden")

    overrideMethodList.clear()
    true

  //Function used to recursively check through the public and protected members of the super classes of the given class to find a method
  //Returns: Some[(LogicGates|Boolean, Seq[String])] i.e method and it's params if method exists and throws RuntimeException if not found
  @tailrec
  def findMethodFromInheritance(className: String, methodName: String, parameters: Int): Option[(LogicGates | Boolean, Seq[String])] =
    findMethod(className, methodName, parameters) match
      case Some(method,params) => Some(method,params)
      case None=> inheritanceMap.get(className) match
        case Some(superClassName) =>
          //Recursively call this function for the superclass
          findMethodFromInheritance(superClassName, methodName, parameters)
        case None => None

  //Function Used to check the existing class Map if the field exists in a particular accessModifier(Also used to detect duplicate field declaration)
  //Return: true if field found in accessModifier, false otherwise
  def checkClassMapForField(className: String, accessModifier: String, fieldName: String): Boolean =
    classMap(className).get(accessModifier) match
        case None => false
        case Some(o1) => o1._1.get(fieldName) match
          case Some(_) => true
          case None => false

  //Function Used to check the existing abstract class Map if the field exists in a particular accessModifier(Also used to detect duplicate field declaration)
  //Return: true if field found in accessModifier, false otherwise
  def checkAbstractClassMapForField(className: String, accessModifier: String, fieldName: String): Boolean =
    abstractClassMap(className).get(accessModifier) match
      case None => false
      case Some(o1) => o1._1.get(fieldName) match
        case Some(_) => true
        case None => false

  //Function Used to check the existing inheritance Map if the field exists in a particular accessModifier(Also used to detect duplicate field declaration)
  //Return: true if field found in accessModifier, false otherwise
  def checkInterfaceMapForField(interfaceName: String, accessModifier: String, fieldName: String): Boolean =
    interfaceMap(interfaceName).get(accessModifier) match
      case None => false
      case Some(o1) => o1._1.get(fieldName) match
        case Some(_) => true
        case None => false

  //Function Used to find field in the interface(It checks through all the access modifiers. Also used to detect duplicate field declaration)
  //Returns: Some(Boolean) i.e value of the field if field exists and None if it does not
  def findFieldInClass(className: String, fieldName: String): Option[Boolean] =
    if checkClassMapForField(className, "Public", fieldName) then
      classMap(className)("Public")._1.get(fieldName) match
        case Some(field) => Some(field)
    else if checkClassMapForField(className, "Protected", fieldName) then
      classMap(className)("Protected")._1.get(fieldName) match
        case Some(field) => Some(field)
    else
      None

  //Function Used to find field in the abstract class(It checks through all the access modifiers. Also used to detect duplicate field declaration)
  //Returns: Some(Boolean) i.e value of the field if field exists and None if it does not
  def findFieldInAbstractClass(className: String, fieldName: String): Option[Boolean] =
    if checkAbstractClassMapForField(className, "Public", fieldName) then
      abstractClassMap(className)("Public")._1.get(fieldName) match
        case Some(field) => Some(field)
    else if checkAbstractClassMapForField(className, "Protected", fieldName) then
      abstractClassMap(className)("Protected")._1.get(fieldName) match
        case Some(field) => Some(field)
    else
      None

  //Function Used to find field in the interface(It checks through all the access modifiers. Also used to detect duplicate field declaration)
  //Returns: Some(Boolean) i.e value of the field if field exists and None if it does not
  def findFieldInInterface(className: String, fieldName: String): Option[Boolean] =
    if checkInterfaceMapForField(className, "Public", fieldName) then
      interfaceMap(currentscopeStack.top)("Public")._1.get(fieldName) match
        case Some(field) => Some(field)
    else if checkInterfaceMapForField(className, "Protected", fieldName) then
      interfaceMap(currentscopeStack.top)("Protected")._1.get(fieldName) match
        case Some(field) => Some(field)
    else
      None

  //Function Used to find field in the abstract class or concrete class or interface(It checks through all the access modifiers. Also used to detect duplicate method declaration)
  //Returns: Some(Boolean) i.e value of the field if field exists and None if it does not
  def findField(className: String, fieldName: String): Option[Boolean] =
    if FindType(className).contains("Class") then
      findFieldInClass(className, fieldName)
    else if FindType(className).contains("Abstract Class") then
      findFieldInAbstractClass(className, fieldName)
    else if FindType(className).contains("Interface") then
      findFieldInInterface(className, fieldName)
    else
      None

  //Function Used to check super classes and interfaces to check if field is found
  def checkSuperClassorInterfaceForField(name: String, fieldName: String, value: Boolean): Unit =
    inheritanceMap.get(name) match
      case Some(superClassName) =>
        if superClassName != "None" then
          setValueForField(superClassName, fieldName, value)
      case None =>
        throw new RuntimeException("Field not found in any of the superclasses")

  //Function used to set Values for the public and protected fields of the class and it's superclasses
  def setValueForField(className: String, fieldName:String, value:Boolean): Unit =
    if FindType(className).contains("Class") then
      findFieldInClass(className, fieldName) match
        case Some(_) =>
          if checkClassMapForField(className, "Public", fieldName) then
            classMap(className)("Public")._1.put(fieldName, value)
          else if checkClassMapForField(className, "Protected", fieldName) then
            classMap(className)("Protected")._1.put(fieldName, value)
        case None =>
          checkSuperClassorInterfaceForField(className, fieldName, value)
    else if FindType(className).contains("Abstract Class") then
      findFieldInAbstractClass(className, fieldName) match
        case Some(_) =>
          if checkAbstractClassMapForField(className, "Public", fieldName) then
            abstractClassMap(className)("Public")._1.put(fieldName, value)
          else if checkAbstractClassMapForField(className, "Protected", fieldName) then
            abstractClassMap(className)("Protected")._1.put(fieldName, value)
        case None =>
          checkSuperClassorInterfaceForField(className, fieldName, value)
    else if FindType(className).contains("Interface") then
      findFieldInAbstractClass(className, fieldName) match
        case Some(_) =>
          if checkInterfaceMapForField(className, "Public", fieldName) then
            interfaceMap(className)("Public")._1.put(fieldName, value)
          else if checkInterfaceMapForField(className, "Protected", fieldName) then
            interfaceMap(className)("Protected")._1.put(fieldName, value)
        case None =>
          checkSuperClassorInterfaceForField(className, fieldName, value)


  //Function used to evaluate a given method, we pass the parameterNames and parameterValues to it and evaluate the method
  def EvaluateMethod(method: LogicGates | Boolean, parameters: Seq[String], parameterValues: Seq[Boolean]): Boolean =
    //We iterate through both Sequences simultaneously using zip
    for ((paramName, paramValue) <- parameters zip parameterValues)
    //We pass the mappings of paramNames and paramValues to the parameter map to evaluate the method
      parameterMap.put(paramName, paramValue)
    method match
      case gate: LogicGates => gate.eval
      case value: Boolean => value

  //This function looks through the current class and it's superclass for the method with the same signature and calls the EvaluateMethod()
  def InvokeMethodFromClass(className: String, methodName: String, parameters: Seq[Boolean]): Boolean =
  //We search for the method in existing class and it's superclasses
    findMethodFromInheritance(className, methodName, parameters.length) match
      case Some(method, params) =>
        EvaluateMethod(method, params, parameters)
      case None => throw new Exception("Method Not Found in any of the superclasses")

  //Function used to recursively check through the public and protected members of the super classes of the given class to find a field
  //Returns: Boolean i.e value of field if field exists and throws RuntimeException if not found
  @tailrec
  def findFieldFromInheritance(className: String, fieldName: String): Option[Boolean] =
    findField(className, fieldName) match
      case Some(field) => Some(field)
      case None =>
        //We retrieve the super class from the inheritance map
        inheritanceMap.get(className) match
          case Some(superClassName) => findFieldFromInheritance(superClassName, fieldName)
          case None => throw new RuntimeException("Field not found in any of the superclasses")

  @tailrec
  //Function used to recursively add the constructors to a stack in order of child-> parent so they can be evaluated in the reverse order
  def AddConstructorToStack(className: String):Unit =
    constructorMap.get(className) match
      case Some(constructor) => constructorStack.push(constructor)
      case None => inheritanceMap.get(className) match
        case Some(superClassName) => AddConstructorToStack(superClassName)
        case None =>

  //Function used to create an object of the class (Since the design is singleton, all new objects are just references to the same object)
  def NewObject(className: String, objectName: String): Unit =
    currentscopeStack.push(className)
    if FindType(className).contains("Class") then
      CheckOverriding(className)
      AddConstructorToStack(className)
      while(constructorStack.nonEmpty)
        val constructorSet: Seq[Assign] = constructorStack.pop()
        for(constructor <- constructorSet)
          constructor.eval
      objectMap.put(objectName, className)
      currentscopeStack.pop()
      overrideMethodList.clear()
    else
      throw new RuntimeException("Objects cannot be created for Abstract Classes and Interfaces (or) Class Does Not Exist")

  def IF(condition: => Boolean, thenClause: => Unit, elseClause: => Unit): Boolean =
  //Do not evaluate if an exception has already occurred.
    if exceptionStack.nonEmpty then
      false
    else
      if condition then thenClause else elseClause
      true

  def CatchException(exceptionClassName: String, tryBlock: => Unit, catchBlock: => Unit): Boolean =
    //Do not evaluate if an exception has already occurred.
    if exceptionStack.nonEmpty then
      false
    else
      tryBlock
      if (exceptionStack.nonEmpty && exceptionStack.top == exceptionClassName)
        val reason: String = exceptionClassMap.get(exceptionClassName) match
          case Some(o1) => o1
          case None => ""
        println("Exception: "+ exceptionClassName + " caught with reason: " + reason)
        exceptionStack.pop()
        catchBlock
        true
      else
         false

  def Scope(scopeName: String, block: => Unit): Boolean =
  //Do not evaluate if an exception has occurred.
    if exceptionStack.nonEmpty then
      false
    else
      scopeStack.push(scopeName)
      block
      classMap.clear()
      abstractClassMap.clear()
      interfaceMap.clear()
      exceptionStack.clear()
      scopeStack.pop()
      true

  def ExceptionClassDef(exceptionClassName: String): Boolean =
    //Push the exceptionClassName onto the map with empty string as reason to be updated later
    exceptionClassMap.put(exceptionClassName, "")
    true

  enum ClassBuilder:
    case Private(defs: ClassBuilder*)
    case Protected(defs: ClassBuilder*)
    case Field(fieldName: String)
    case Method(methodName: String, expr: LogicGates | Boolean, parameters: String*)
    case AbstractMethod(methodName: String, parameters: String*)
    case Assign(fieldName: String, value: Boolean)
    case Constructor(arguments: Assign*)
    case ClassDef(className: String, defs: ClassBuilder*)
    case InvokeMethod(objectName: String, methodName: String, parameters: Boolean*)
    case Extends(className: String)
    case Implements(interfaces: String*)
    case SetValueForField(className:String, fieldName:String, value:Boolean)
    case AbstractClassDef(className: String, defs: ClassBuilder*)
    case InterfaceDef(interfaceName: String, defs: ClassBuilder*)
    case ThrowException(exceptionClassName: String, reason: String)
    case Print(o1: LogicGates| Boolean)


    def AddToMap(modifierString:String, typeString:String):Unit =
      if typeString ==  "Class" then
        AddToClassMap(modifierString)
      else if typeString == "Abstract Class" then
        AddToAbstractClassMap(modifierString)
      else
        AddToInterfaceMap(modifierString)

    //Function used to add the fieldMap and methodMap of each access modifier into it's classAccessMap. Then we add the classAccessMap onto the classMap
    def AddToClassMap(modifierString: String): Unit =
      val var1: fieldMapType = fieldMap.clone()
      val var2: methodMapType = methodMap.clone()
      classAccessMap.put(modifierString, (var1, var2))
      val var3: classAccessMapType = classAccessMap.clone()
      classMap.get(currentscopeStack.top) match
        case Some(map) => classMap.put(currentscopeStack.top, map ++ var3)
        case None => classMap.put(currentscopeStack.top, var3)
      classAccessMap.clear()
      fieldMap.clear()
      methodMap.clear()

    //Function used to add the fieldMap and methodMap of each access modifier into it's classAccessMap. Then we add the classAccessMap onto the classMap
    def AddToAbstractClassMap(modifierString: String): Unit =
      val var1: fieldMapType = fieldMap.clone()
      val var2: methodMapType = methodMap.clone()
      val var3: abstractMethodListType = abstractMethodList.clone()
      abstractClassAccessMap.put(modifierString, (var1, var2, var3))
      val var4: abstractClassAccessMapType = abstractClassAccessMap.clone()
      abstractClassMap.get(currentscopeStack.top) match
        case Some(map) => abstractClassMap.put(currentscopeStack.top, map ++ var4)
        case None => abstractClassMap.put(currentscopeStack.top, var4)
      abstractMethodList.clear()
      fieldMap.clear()
      abstractClassAccessMap.clear()
      methodMap.clear()


    //Function used to add the fieldMap and methodMap of each access modifier into it's classAccessMap. Then we add the classAccessMap onto the classMap
    def AddToInterfaceMap(modifierString: String): Unit =
      val var1: fieldMapType = fieldMap.clone()
      val var2: abstractMethodListType = abstractMethodList.clone()
      interfaceAccessMap.put(modifierString, (var1, var2))
      val var3: interfaceAccessMapType = interfaceAccessMap.clone()
      interfaceMap.get(currentscopeStack.top) match
        case Some(map) => interfaceMap.put(currentscopeStack.top, map ++ var3)
        case None => interfaceMap.put(currentscopeStack.top, var3)
      abstractMethodList.clear()
      interfaceAccessMap.clear()
      fieldMap.clear()


    def ProcessClassorInterface():Unit =
      //If it is not enclosed in private or protected we take it as public
      AddToMap("Public", typeStack.top)
      //Remove all elements from the maps and remove the top element in stack
      inheritanceMap.get(currentscopeStack.top) match
        case Some(_) => //Extends has been called already and the inheritance map already has an entry
        case None =>
          //Extends was not used for this class and it does not inherit from any other class
          inheritanceMap.put(currentscopeStack.top, "None")
      currentscopeStack.pop()
      typeStack.pop()

    def eval: Boolean =
    //Do not evaluate if an exception has occurred.
      if exceptionStack.nonEmpty then
        true
      else
        this match
        case ClassDef(o1, o2*) =>
          typeStack.push("Class")
          classMap.get(o1) match
            case Some(_) =>
              //If entry already exists in the classMap we do not allow redefinition
              throw new RuntimeException("Redefinition of Class")
            case None =>
              //If there is no entry we can proceed
              currentscopeStack.push(o1)
          for(arg <- o2)
            arg.eval
          ProcessClassorInterface()
          true

        case AbstractClassDef(o1, o2*) =>
          typeStack.push("Abstract Class")
          abstractClassMap.get(o1) match
            case Some(_) =>
              //If entry already exists in the classMap we do not allow redefinition
              throw new RuntimeException("Redefinition of Abstract Class")
            case None =>
              //If there is no entry we can proceed
              currentscopeStack.push(o1)
          for (arg <- o2)
            arg.eval
          ProcessClassorInterface()
          true

        case InterfaceDef(o1, o2*) =>
          typeStack.push("Interface")
          inheritanceMap.get(o1) match
            case Some(_) =>
              //If entry already exists in the classMap we do not allow redefinition
              throw new RuntimeException("Redefinition of Interface")
            case None =>
              //If there is no entry we can proceed
              currentscopeStack.push(o1)
          for (arg <- o2)
            arg.eval
          ProcessClassorInterface()
          true

        case Field(o1) =>
          //All fields are initialized to false during creation
          //Check if Field already exists in another access modifier
          findField(currentscopeStack.top, o1) match
            case Some(_) =>
              //If it already exists we cannot declare a duplicate field
              throw new RuntimeException("Duplicate Field Declaration")
            case None =>
              //If it does not exist we add the field
              fieldMap.put(o1, false)
          true

        case Method(o1,o2,o3*) =>
          if typeStack.top == "Class" || typeStack.top == "Abstract Class" then
              findMethod(currentscopeStack.top, o1, o3.length) match
                case Some(_,_) =>
                  throw new Exception("Duplicate Declaration of Method")
                case None =>
                  methodMap.put((o1, o3.length), (o2,o3))
                  true
          else
            throw new RuntimeException("Concrete Methods cannot be defined for Interfaces")

        case AbstractMethod(o1, o2*) =>
          if typeStack.top == "Abstract Class" || typeStack.top == "Interface" then
                abstractMethodList.add((o1, o2.length))
                true
          else
            throw new RuntimeException("Abstract Methods cannot be defined for Concrete Classes")

        case Constructor(o1*) =>
          if typeStack.top == "Interface" then
            throw new Exception("Constructors cannot be declared in interfaces")
          constructorMap.put(currentscopeStack.top, o1)
          true

        case Assign(o1,o2) =>
          //Set the value of field in the constructor
          SetValueForField(currentscopeStack.top, o1, o2).eval
          true

        case Private(o1*) =>
          //Adding all the private fields and methods onto the map
          accessModifierStack.push("Private")
          for(arg <- o1)
            arg.eval

          AddToMap("Private", typeStack.top)
          accessModifierStack.pop()
          true

        case Protected(o1*) =>
          //Adding all the protected fields and methods onto the map
          accessModifierStack.push("Protected")
          for (arg <- o1)
            arg.eval
          AddToMap("Protected", typeStack.top)
          accessModifierStack.pop()
          true

        case Extends(o1) =>
          if (o1 == currentscopeStack.top)
          //Class Cannot Extend Itself
            throw new RuntimeException("Class or Interface Cannot Inherit Itself")
          inheritanceMap.get(currentscopeStack.top) match
            case Some(_) =>
              //If an entry for this class or interface already exists in the inheritance map it means it already extends some class, we cannot allow it to extend another class
              throw new RuntimeException("Multiple Inheritance is not allowed")
            case None =>
              if typeStack.top == "Interface" then
                interfaceMap.get(o1) match
                  //If the interface exists in interfacemap we add it to the inheritance map
                  case Some(_) => inheritanceMap.put(currentscopeStack.top, o1)
                  case None => throw new Exception("Extends called on a non-interface or interface doesn't exist")
              else
                if FindClass(o1) then
                  //If abstract or concrete class o1 exists we put it on the inheritance map
                  inheritanceMap.put(currentscopeStack.top, o1)
                else
                  throw new RuntimeException("Class Inherited From Does Not Exist")

          true

        case Implements(o1*) =>
          if typeStack.top == "Interface" then
            throw new RuntimeException("Interface cannot implement another interface")
          else
            val implementSet: collection.mutable.Set[String] = collection.mutable.Set()
            for(args <- o1)
              if interfaceMap.contains(args) then
                implementSet += args
              else
                throw new RuntimeException("Interface Implemented From Does Not Exist")
            implementsList.put(currentscopeStack.top, implementSet)
            true

        case SetValueForField(o1,o2,o3) =>
          setValueForField(o1,o2,o3)
          true

        case InvokeMethod(o1,o2,o3*) =>
          //Only Protected and Public Methods can be called
          val className:String = classMap.get(o1) match
            case Some(_) =>
              //If o1 exists in the classMap it's a valid className
              o1
            case None =>
              //If o1 is not a className we check if it's an object name from the objectMap
              objectMap.get(o1) match
                case Some(className) => className
                case None => throw new RuntimeException("No Class or Object Exists with this Name")

          currentscopeStack.push(className)
          val result: Boolean = InvokeMethodFromClass(className,o2,o3)
          currentscopeStack.pop()
          parameterMap.clear()
          result

        case ThrowException(exceptionClassName: String, reason: String) =>
          //Push the exceptionClassName and reason onto the exception stack
          exceptionStack.push(exceptionClassName)
          //Push the reason onto the exceptionClassMap
          exceptionClassMap.put(exceptionClassName,reason)
          true

        case Print(o1) =>
          println(o1)
          true

  //This function is used to match the union and appropriately handle the cases
  def matchType(o1: LogicGates | Boolean):  Boolean =
    o1 match
      case Variable(variableName) =>
        retrieveVariable(variableName) match
          case a: Boolean => a
          case _: Variable => throw new RuntimeException("Variable not yet defined")
      case Input(variableName) =>
        //Search in the parameterMap
        parameterMap.get(variableName) match
          case Some(x) => x
          case None => throw new Exception("No value for parameter passed to function")
      case ClassField(fieldName) =>
        //Find in field in classMap and inheritance
        findFieldInClass(currentscopeStack.top, fieldName) match
          case Some(x) => x
          case None =>
            findFieldFromInheritance(currentscopeStack.top, fieldName) match
              case Some(x) => x
      case a: LogicGates => a.eval
      case a: Boolean => a

  //This function checks if partial evaluation is required for a given expression
  def checkPartialEvaluation(expr: LogicGates) : Boolean =
    try
      expr.eval
      //If there is no exception this means the expression can fully evaluate to a Boolean
      false
    catch
      //If a RuntimeException occurs it means one or more of the inputs are not defined
      case _: RuntimeException => true

  //Function to find the variable in global scope
  def checkGlobalScope(variable: String): Boolean | Variable =
    scopeMap.get(("Global", variable)) match
      case Some(x) => x
      case None => Variable(variable)

  //Function that returns the value of Variable, if no value is present returns back Variable(variableName)
  def retrieveVariable(variableName: String) : Boolean | Variable =
    if scopeStack.nonEmpty then
      scopeMap.get((scopeStack.top, variableName)) match
        case Some(x) => x
        case None => checkGlobalScope(variableName)
    else
      checkGlobalScope(variableName)

  //Recursive function to evaluate partial expressions
  def matchCase(o1: LogicGates | Boolean):  Boolean| LogicGates =
    o1 match
      case Variable(variableName) => retrieveVariable(variableName)
      case Input(variableName) => matchType(Input(variableName))
      case ClassField(fieldName) => matchType(ClassField(fieldName))
      case a: LogicGates => partialEvaluate(a)
      case a: Boolean => a

  def partialEvaluate(expr: LogicGates) : LogicGates | Boolean =
    //Here we recursively evaluate the partial expression
    expr match
      case AND(o1,o2) => AND(matchCase(o1), matchCase(o2))
      case OR(o1,o2) => OR(matchCase(o1), matchCase(o2))
      case NAND(o1,o2) => NAND(matchCase(o1), matchCase(o2))
      case NOR(o1,o2) => NOR(matchCase(o1), matchCase(o2))
      case XOR(o1,o2) => XOR(matchCase(o1), matchCase(o2))
      case XNOR(o1,o2) => XNOR(matchCase(o1), matchCase(o2))
      case NOT(o1) => NOT(matchCase(o1))

  def simplifyExpression(expr: LogicGates| Boolean) : LogicGates| Boolean =
    //Here we apply the optimization functions to reduce the partial expressions through the monadic function map
    expr match
      case a:Boolean => a
      case Variable(o1) => Variable(o1)
      case AND(o1, o2) =>
        //Here we apply the optimization function simplifyAND to reduce the partial expressions through the monadic function map
        map(AND(o1,o2), simplifyAND) match
        case a:Boolean => a
        case AND(o1,o2) => AND(simplifyExpression(o1), simplifyExpression(o2))
      case OR(o1, o2) =>
        //Here we apply the optimization function simplifyOR to reduce the partial expressions through the monadic function map
        map(OR(o1, o2), simplifyOR) match
        case a: Boolean => a
        case OR(o1, o2) => OR(simplifyExpression(o1), simplifyExpression(o2))
      case NAND(o1, o2) =>
        //Here we apply the optimization function simplifyNAND to reduce the partial expressions through the monadic function map
        map(NAND(o1, o2), simplifyNAND) match
        case a: Boolean => a
        case NAND(o1, o2) => NAND(simplifyExpression(o1), simplifyExpression(o2))
      case NOR(o1, o2) =>
        //Here we apply the optimization function simplifyNOR to reduce the partial expressions through the monadic function map
        map(NOR(o1, o2), simplifyNOR) match
        case a: Boolean => a
        case NOR(o1, o2) => NOR(simplifyExpression(o1), simplifyExpression(o2))
      case XOR(o1, o2) => XOR(simplifyExpression(o1),simplifyExpression(o2))
      case XNOR(o1, o2) => XNOR(simplifyExpression(o1),simplifyExpression(o2))
      case NOT(o1) => o1 match
        case a: Boolean => !a
        case NOT(o1) => NOT(simplifyExpression(o1))

  //monadic function map that applies the optimization functions that are passed as the second parameter
  def map(expr: LogicGates, f: LogicGates => LogicGates| Boolean): LogicGates| Boolean =
    f(expr)

  enum LogicGates:
    case AND(o1: LogicGates | Boolean, o2: LogicGates | Boolean)
    case OR(o1: LogicGates | Boolean, o2: LogicGates| Boolean)
    case NAND(o1: LogicGates | Boolean, o2: LogicGates | Boolean)
    case NOR(o1: LogicGates | Boolean , o2: LogicGates | Boolean)
    case XOR(o1: LogicGates | Boolean , o2: LogicGates | Boolean)
    case XNOR(o1: LogicGates |Boolean, o2: LogicGates | Boolean)
    case NOT(o1: LogicGates | Boolean)
    case Input(name: String)
    case ClassField(name: String)
    case Variable(name: String)

    def evaluate: LogicGates | Boolean =
      if !checkPartialEvaluation(this) then
        this.eval
      else
      //One or more of the inputs are not defined we need to partially evaluate and simplify
        simplifyExpression(partialEvaluate(this))


    def eval: Boolean  =
      this match
      case AND(o1, o2) =>
        if (matchType(o1) && matchType(o2)) true else false
      case OR(o1, o2) =>
        if (matchType(o1) || matchType(o2)) true else false
      case NAND(o1, o2) =>
        if (matchType(o1) && matchType(o2)) false else true
      case NOR(o1, o2) =>
        if (matchType(o1) || matchType(o2)) false else true
      case XOR(o1, o2) =>
        if (matchType(o1) != matchType(o2)) true else false
      case XNOR(o1, o2) =>
        if (matchType(o1) == matchType(o2)) true else false
      case NOT(o1) =>
        if (matchType(o1)) false else true


  //This inserts the value in the given scope, if no scope specified, inserts in global scope
  def Insert(variableName: String, value: Boolean): Unit =
    if (scopeStack.isEmpty)
      scopeMap.put(("Global", variableName), value)
    else
      scopeMap.put((scopeStack.top, variableName), value)