// This program is copyright VUW.
// You are granted permission to use it to construct your answer to a COMP103 assignment.
// You may not distribute it in any other way without permission.

/* Code for COMP103 - 2023T2, Assignment 5
 * Name:
 * Username:
 * ID:
 */

import ecs100.*;
import java.awt.Color;
import java.util.*;
import java.io.*;
import java.nio.file.*;

/** 
 * Calculator for Cambridge-Polish Notation expressions
 * (see the description in the assignment page)
 * User can type in an expression (in CPN) and the program
 * will compute and print out the value of the expression.
 * The template provides the method to read an expression and turn it into a tree.
 * You have to write the method to evaluate an expression tree.
 *  and also check and report certain kinds of invalid expressions
 */

public class CPNCalculator{

    /**
     * Setup GUI then run the calculator
     */
    public static void main(String[] args){
        CPNCalculator calc = new CPNCalculator();
        calc.setupGUI();
        calc.runCalculator();
    }

    /** Setup the gui */
    public void setupGUI(){
        UI.addButton("Clear", UI::clearText); 
        UI.addButton("Quit", UI::quit); 
        UI.setDivider(1.0);
    }

    /**
     * Run the calculator:
     * loop forever:  (a REPL - Read Eval Print Loop)
     *  - read an expression,
     *  - evaluate the expression,
     *  - print out the value
     * Invalid expressions could cause errors when reading or evaluating
     * The try-catch prevents these errors from crashing the program - 
     *  the error is caught, and a message printed, then the loop continues.
     */
    public void runCalculator(){
        UI.println("Enter expressions in pre-order format with spaces");
        UI.println("eg   ( * ( + 4 5 8 3 -10 ) 7 ( / 6 4 ) 18 )");
        while (true){
            UI.println();
            try {
                GTNode<ExpElem> expr = readExpr();
                double value = evaluate(expr);
                UI.println(" -> " + value);
            }catch(Exception e){UI.println("Something went wrong! "+e);}
        }
    }

    /**
     * Evaluate an expression and return the value
     * Returns Double.NaN if the expression is invalid in some way.
     * If the node is a number
     *  => just return the value of the number
     * or it is a named constant
     *  => return the appropriate value
     * or it is an operator node with children
     *  => evaluate all the children and then apply the operator.
     */
    public double evaluate(GTNode<ExpElem> expr){
        if (expr==null){
            return Double.NaN;
        }

        switch (expr.getItem().operator) {
            case "#":
                return expr.getItem().value;
            case "PI":
                return Math.PI;
            case "E":
                return Math.E;
            case "+":
                return add(expr);
            case "-":
                return subtract(expr);
            case "*":
                return multiply(expr);
            case "/":
                return divide(expr);
            case "sqrt":
                return Math.sqrt(evaluate(expr.getChild(0)));
            case "log":
                return log(expr);
            case "ln":
                return Math.log(evaluate(expr.getChild(0)));
            case "sin":
                return Math.sin(evaluate(expr.getChild(0)));
            case "cos":
                return Math.cos(evaluate(expr.getChild(0)));
            case "tan":
                return Math.tan(evaluate(expr.getChild(0)));
            case "dist":
                return dist(expr);
            case "avg":
                return add(expr) / expr.numberOfChildren();
            default:
                UI.println("The operator " + expr.getItem().operator + "is invalid");
                return Double.NaN;
        }
    }

    /**
     * Adds up evaluated children
     * @param node the node to start from (should be a + )
     * @return the result
     */
    public double add(GTNode<ExpElem> node) {
        double total = 0;
        for (GTNode<ExpElem> child : node) {
            total += evaluate(child);
        }
        return total;
    }

    /**
     * Subtracts from the first operand (evaluated child node) all other operands
     * @param node node to start from (-)
     * @return the result
     */
    public double subtract(GTNode<ExpElem> node) {
        //2* adds an extra copy (which gets taken away) so can use normal foreach
        double result = 2 * evaluate(node.getChild(0));
        for (GTNode<ExpElem> child : node) {
            result -= evaluate(child);
        }
        return result;
    }

    /**
     * Multiplies all the operands (evaluated children) together
     * @param node the "root" operator node (*)
     * @return result
     */
    public double multiply(GTNode<ExpElem> node) {
        double result = 1;
        for (GTNode<ExpElem> child : node) {
            result *= evaluate(child);
        }
        return result;
    }

    /**
     * Divides the first operand by all the remaining operands
     * @param node the "root" operator node (/)
     * @return result
     */
    public double divide(GTNode<ExpElem> node) {
        double result = Math.pow(evaluate(node.getChild(0)), 2); //squared for normal foreach
        for (GTNode<ExpElem> child : node) {
            result /= evaluate(child);
        }
        return result;
    }

    public double log(GTNode<ExpElem> node) {
        if (node.numberOfChildren() == 1) {
            return Math.log10(evaluate(node.getChild(0)));
        }
        else {
            double base = evaluate(node.getChild(0));
            double operand = evaluate(node.getChild(0);
            return Math.log(operand) / Math.log(base); // log rules :D
        }
    }

    public double dist(GTNode<ExpElem> node) {
        if (node.numberOfChildren() == 4) {
            double xDist = evaluate(node.getChild(2)) - evaluate(node.getChild(0));
            double yDist = evaluate(node.getChild(3)) - evaluate(node.getChild(1));
            return Math.sqrt(Math.pow(xDist, 2) + Math.pow(yDist, 2));
        }
        else if (node.numberOfChildren() == 6) {
            double xDist = evaluate(node.getChild(3)) - evaluate(node.getChild(0));
            double yDist = evaluate(node.getChild(4)) - evaluate(node.getChild(1));
            double zDist = evaluate(node.getChild(5)) - evaluate(node.getChild(2));
            return Math.sqrt(Math.pow(xDist, 2) + Math.pow(yDist, 2) + Math.pow(zDist, 2));
        }
        else return Double.NaN;
    }

    /** 
     * Reads an expression from the user and constructs the tree.
     */ 
    public GTNode<ExpElem> readExpr(){
        String expr = UI.askString("expr:");
        return readExpr(new Scanner(expr));   // the recursive reading method
    }

    /**
     * Recursive helper method.
     * Uses the hasNext(String pattern) method for the Scanner to peek at next token
     */
    public GTNode<ExpElem> readExpr(Scanner sc){
        if (sc.hasNextDouble()) {                     // next token is a number: return a new node
            return new GTNode<ExpElem>(new ExpElem(sc.nextDouble()));
        }
        else if (sc.hasNext("\\(")) {                 // next token is an opening bracket
            sc.next();                                // read and throw away the opening '('
            ExpElem opElem = new ExpElem(sc.next());  // read the operator
            GTNode<ExpElem> node = new GTNode<ExpElem>(opElem);  // make the node, with the operator in it.
            while (! sc.hasNext("\\)")){              // loop until the closing ')'
                GTNode<ExpElem> child = readExpr(sc); // read each operand/argument
                node.addChild(child);                 // and add as a child of the node
            }
            sc.next();                                // read and throw away the closing ')'
            return node;
        }
        else {                                        // next token must be a named constant (PI or E)
                                                      // make a token with the name as the "operator"
            return new GTNode<ExpElem>(new ExpElem(sc.next()));
        }
    }

}

