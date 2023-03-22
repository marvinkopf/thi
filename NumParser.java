import java.io.FileReader;
import java.util.LinkedList;
import java.util.Stack;
import java.util.function.Predicate;

/*
  NumParser.java
  
  Beispiel zur Vorlesung
  
  Realisiert die folgende kontextfreie Grammatik f�r Strings aus Ziffern
  num -> digit num | num
  digit -> '1'|'2'|'3'|'4'|'5'|'6'|'7'|'8'|'9'|'0'
  
  Der Parser ist nach dem Prinzip des rekursiven Abstiegs programmiert,
  d.h. jedes nicht terminale Symbol der Grammatik wird durch eine 
  Methode in Java repr�sentiert, die die jeweils anderen nicht terminalen
  Symbole auf der rechten Seite der Grammatik Regeln ggf. auch rekursiv
  aufruft.
  
  Der zu parsende Ausdruck wird aus einer Datei gelesen und in einem
  Array of Char abgespeichert. Pointer zeigt beim Parsen auf den aktuellen
  Eingabewert.
  
  Ist der zu parsende Ausdruck syntaktisch nicht korrekt, so werden 
  �ber die Methode syntaxError() entsprechende Fehlermeldungen ausgegeben

*/

class NumParser{
    // Konstante f�r Ende der Eingabe

    // Anfang Attribute
    static final char EOF=(char)255;

    //-------------------------------------------------------------------------
    // Methode zum korrekt einger�ckten Ausgeben des Syntaxbaumes auf der
    // Konsole
    //
    // Der Parameter s �bergibt die Beschreibung des Knotens als String
    // Der Parameter t �bergibt die Einr�ck-Tiefe
    //-------------------------------------------------------------------------

    static void ausgabe(String s, int t){
        for(int i=0;i<t;i++)
            System.out.print("    ");
        System.out.println(s);
    }//ausgabe

    static boolean syntaxError(int t, String c){
        System.out.println("Syntax Fehler beim "+t+" Zeichen: "
                +c);
        return false;
    }//syntaxError

    static class Rule {
        public Rule(Predicate<Integer> condition, Predicate<Integer> action) {
            this.condition = condition;
            this.action = action;
        }

        Predicate<Integer> condition;
        Predicate<Integer> action;
    }

    static class NonTerminal {
        LinkedList<Rule> rules = new LinkedList<>();

        public NonTerminal(String name) {
            this.name = name;
        }

        String name;

        boolean call(int t) {
            ausgabe(name +"->", t);
            for(Rule r : rules)
                if(r.condition.test(t)) {
                    return r.action.test(t + 1);
                }

            // Sollte nie erreicht werden.
            return false;
        }

        void addRule(Predicate<Integer> condition, Predicate<Integer> action) {
            rules.add(new Rule(condition, action));
        }
    }

    static boolean mat(char[] terminals, char[] input, int t) {
        if (new String(terminals).contains(String.valueOf(input[pointer]))) {
            ausgabe(" match: " + input[pointer], t+1);
            pointer++;
            return true;
        }
        return false;
    }

    //-------------------------------------------------------------------------
    // Main Methode, startet den Parser und gibt das ERgebnis des Parser-
    // durchlaufs auf der Konsole aus
    //-------------------------------------------------------------------------
    static int pointer = 0;
    static int maxPointer = 0;

    static boolean readInput(String name){
        int c=0;
        try{
            FileReader f = new FileReader(name);
            for(int i=0;i<256;i++){
                c = f.read();
                if (c== -1){
                    maxPointer=i;
                    input[i]=EOF;
                    break;
                }else
                    input[i]=(char)c;
            }
        }
        catch(Exception e){
            System.out.println("Fehler beim Dateizugriff: "+name);
            return false;
        }
        return true;
    }//readInput

      //-------------------------------------------------------------------------
  // Methode, die testet, ob das Ende der Eingabe erreicht ist
  // (pointer == maxPointer)
  //------------------------------------------------------------------------- 

  static boolean inputEmpty(){
    if (pointer==maxPointer){
      ausgabe("Eingabe leer!",0);
      return true;
    }else{
      syntaxError(pointer+1,"Eingabe bei Ende des Parserdurchlaufs nicht leer");
      return false;
    }
  }//inputEmpty


    static char[] input;
    public static void main(String args[]){
        char[] digitMatches = {'1','2','3','4','5','6','7','8','9','0'};
        input = new char[256];

        readInput("testdatei.txt");

        var expression = new NonTerminal("expression");
        var rightExpression = new NonTerminal("rightExpression");
        var term = new NonTerminal("term");
        var rightTerm = new NonTerminal("rightTerm");
        var operator = new NonTerminal("operator");
        var num = new NonTerminal("num");
        var digit = new NonTerminal("digit");

        // Expression
        expression.addRule(t -> true, t -> term.call(t) && rightExpression.call(t));
        // Expression end

        // rightExpression
        rightExpression.addRule(t -> input[pointer] == '+' ||
                        input[pointer] == '-',
                t-> {
                    ausgabe("'"+input[pointer]+"'", t);
                    pointer++;
                    return term.call(t) && rightExpression.call(t);
                });
        rightExpression.addRule(t -> true, t -> {
            ausgabe("epsilon", t);
            return true;
        });
        // rightExpression end

        // Term
        term.addRule(t -> true, t->operator.call(t) && rightTerm.call(t));
        // Term end

        // RightTerm
        rightTerm.addRule(t -> input[pointer] == '*' ||
                        input[pointer] == '/',
                t-> {
                    ausgabe("'"+input[pointer]+"'", t);
                    pointer++;
                    return operator.call(t) && rightTerm.call(t);
                });
        rightTerm.addRule(t -> true,
                t -> {
                    ausgabe("epsilon", t);
                    return true;
                });
        // RightTerm end


        // Operator
        operator.addRule(t -> input[pointer] == '(',
                t -> {
                    ausgabe("match (", t);
                    pointer++;
                    if(expression.call(t) && input[pointer] == EOF) {
                        syntaxError(pointer+1, "Geschlossene Klammer erwartet.");
                        return false;
                    }
                    if(input[pointer] == ')') {
                        pointer++;
                        ausgabe("match )", t);
                        return true;
                    }
                    return false;
                });
        operator.addRule(t -> true, t -> num.call(t));
        // Operator end

        // Num
        num.rules.add(new Rule(t -> new String(digitMatches).contains(String.valueOf(input[pointer + 1])),
                t -> digit.call(t) && num.call(t)));
        num.rules.add(new Rule(t -> true, t -> digit.call(t)));
        // Num end

        // Digit
        digit.addRule(t -> mat(digitMatches, input, t), t -> true);
        digit.addRule(t -> true, t -> syntaxError(pointer+1, String.valueOf(input[pointer])));
        // Digit end

    // Einlesen der Datei und Aufruf des Parsers    
    if (readInput("testdatei.txt"))
      if (expression.call(0)&& inputEmpty())
        System.out.println("Korrekter Ausdruck");
      else
        System.out.println("Fehler im Ausdruck"); 

    }//main
    // Ende Methoden
}