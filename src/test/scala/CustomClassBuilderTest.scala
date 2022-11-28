import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import CustomClassBuilder.*
import CustomClassBuilder.ClassBuilder.*
import CustomClassBuilder.LogicGates.*

class CustomClassBuilderTest extends AnyFlatSpec with Matchers {

  behavior of "my first language for Class Operation"

  "My Language" should "Have Scopes, Branching Statements and Throw/Catch Exceptions and showcase partial evaluation" in {
    Scope("scope1", {
      //We create an interface with 3 methods
      InterfaceDef("Interface 1", Protected(AbstractMethod("m1", "A", "B"), AbstractMethod("m2")), AbstractMethod("m3")).eval
      //We override all the methods of the interface we implement
      ClassDef("Class 1", Private(Field("D"), Method("m5", NOR(true, OR(false, Input("A"))), "A")), Implements("Interface 1"), Constructor(Assign("A", true)), Field("A"), Field("B"), Method("m1", XOR(XNOR(Input("A"), true), NAND(Input("B"), false)), "A", "B"), Method("m2", true), Method("m3", AND(ClassField("A"), NOT(ClassField("B"))))).eval
      //We create an object of Class 1
      NewObject("Class 1", "A")
      //Invoke method m3 of Class 1
      InvokeMethod("A", "m3").eval shouldBe true
      //We set value for field B of class 1
      SetValueForField("Class 1", "B", true)
      //We call m3 now where value of B will be true
      InvokeMethod("A", "m3").eval shouldBe true
      //Partial Evaluation
      IF({// Condition based on which then or else block will be evaluated
        Insert("A", true)
        Insert("D", false)
        //here we showcase partial evaluation and optimization, (variables C and E are not defined) but we optimize the gates OR and NAND and reduce the expression to AND(OR(true,Variable(C)), NAND(false,Variable(E))) which is reduced to AND(true,true)
        AND(OR(Variable("A"), Variable("C")), NAND(Variable("D"), Variable("E"))).evaluate.equals(true)
      }, {// then block
        //We print out the value of this expression, which results in a reduced partial expression
        Print(AND(OR(Variable("A"), Variable("C")), NAND(Variable("X"), Variable("E"))).evaluate).eval
      }, { //else block
        //this block will not execute so we can have a false assertion
          true shouldBe false
      })
    })
    Scope("scope2", {
      ExceptionClassDef("runtimeExceptionA")
      CatchException("runtimeExceptionA", { //try block
        IF({ //condition
          AND(true, false).eval
        }, { //then block

        }, { //else block
          //We create an interface with 3 methods
          InterfaceDef("Interface 2", Protected(AbstractMethod("m1", "A", "B"), AbstractMethod("m2")), AbstractMethod("m3")).eval
          //We override all the methods of the interface we implement
          ClassDef("Class 2", Implements("Interface 2"), Field("A"), Field("B"), Method("m1", XOR(XNOR(Input("A"), true), NAND(Input("B"), false)), "A", "B"), Method("m2", true), Method("m3", AND(ClassField("A"), NOT(ClassField("B"))))).eval
          //We create an object of Class 1
          NewObject("Class 2", "A")
          ThrowException("runtimeExceptionA", "Throwing an Error to be caught in Catch Block").eval
        })
      }, { //catch block
        //since we throw an exception of the type that we are catching the catch block gets executed
        InvokeMethod("A", "m3").eval shouldBe false
      })
    })
  }

  "The Language" should "Showcase Partial Evaluation" in {
    Scope("scope2", {
      IF({ //condition block
        AND(true,false).eval
      },{//then block
        Insert("A", true)
        Insert("B", true)
      },{//else block
        Insert("A", false)
        Insert("B", false)
      })
      //We showcase here that partial evaluation has reduced this expression XOR(OR(NOT(Variable("A")), NAND(Variable("B"),Variable("C"))),Variable("D")) into XOR(OR(true,true),Variable("D"))
      XOR(OR(NOT(Variable("A")), NAND(Variable("B"),Variable("C"))),Variable("D")).evaluate.equals(XOR(OR(true,true),Variable("D"))) shouldBe true
    })
  }

  "This Language" should "Throw an exception if it cannot reduce through further recursive simplification" in {
    assertThrows[Exception] {
      Scope("scopeA", {
        Insert("A", false)
        Insert("B", false)
        // We know that the following expression should reduce to XOR(OR(true,true),Variable("D")) and this can be further reduced to XOR(true, Variable("D")) but my implementation cannot reduce an already reduced expression
        XOR(OR(NOT(Variable("A")), NAND(Variable("B"), Variable("C"))), Variable("D")).equals(XOR(true, Variable("D"))) shouldBe true
      })
    }
  }

  "Language" should "Placing a partially evaluated expression in a condition block causes an exception" in {

    assertThrows[RuntimeException] {
       IF({//condition
         // The variable D is not defined here so this will result in an exception
         XOR(true, Variable("D")).eval
       },{//then block
          ClassDef("Class 1", Field("A")).eval
       },{ //else block
          ClassDef("Class 2", Field("A")).eval
       })
    }
  }

  "Our Language" should "Have nested CatchException Blocks" in {
    Scope("scope1", {
      ExceptionClassDef("runtimeExceptionA")
      CatchException("runtimeExceptionA", { //try block
        IF({ //condition
          AND(true, false).eval
        }, { //then block

        }, { //else block
          //We create an interface with 3 methods
          InterfaceDef("Interface 3", Protected(AbstractMethod("m1", "A", "B"), AbstractMethod("m2")), AbstractMethod("m3")).eval
          //We override all the methods of the interface we implement
          ClassDef("Class 3", Implements("Interface 3"), Field("A"), Field("B"), Method("m1", XOR(XNOR(Input("A"), true), NAND(Input("B"), false)), "A", "B"), Method("m2", true), Method("m3", AND(ClassField("A"), NOT(ClassField("B"))))).eval
          //We create an object of Class 1
          NewObject("Class 3", "A")
          CatchException("runtimeExceptionB",{//try block
            ThrowException("runtimeExceptionA", "throwing exception to outer exception block to be caught").eval
          },
          {//catch block
            //this block does not get evaluated so it won't cause an error
            true shouldBe false
          })
        })
      }, { //catch block
        //since we throw an exception inside a nested catch block we are still able to catch it in an outer catch block
        InvokeMethod("A", "m3").eval shouldBe false
      })
    })
  }
  

}