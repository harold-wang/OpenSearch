/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.renameme.painless.phase;

import org.renameme.painless.node.EAssignment;
import org.renameme.painless.node.EBinary;
import org.renameme.painless.node.EBooleanComp;
import org.renameme.painless.node.EBooleanConstant;
import org.renameme.painless.node.EBrace;
import org.renameme.painless.node.ECall;
import org.renameme.painless.node.ECallLocal;
import org.renameme.painless.node.EComp;
import org.renameme.painless.node.EConditional;
import org.renameme.painless.node.EDecimal;
import org.renameme.painless.node.EDot;
import org.renameme.painless.node.EElvis;
import org.renameme.painless.node.EExplicit;
import org.renameme.painless.node.EFunctionRef;
import org.renameme.painless.node.EInstanceof;
import org.renameme.painless.node.ELambda;
import org.renameme.painless.node.EListInit;
import org.renameme.painless.node.EMapInit;
import org.renameme.painless.node.ENewArray;
import org.renameme.painless.node.ENewArrayFunctionRef;
import org.renameme.painless.node.ENewObj;
import org.renameme.painless.node.ENull;
import org.renameme.painless.node.ENumeric;
import org.renameme.painless.node.ERegex;
import org.renameme.painless.node.EString;
import org.renameme.painless.node.ESymbol;
import org.renameme.painless.node.EUnary;
import org.renameme.painless.node.SBlock;
import org.renameme.painless.node.SBreak;
import org.renameme.painless.node.SCatch;
import org.renameme.painless.node.SClass;
import org.renameme.painless.node.SContinue;
import org.renameme.painless.node.SDeclBlock;
import org.renameme.painless.node.SDeclaration;
import org.renameme.painless.node.SDo;
import org.renameme.painless.node.SEach;
import org.renameme.painless.node.SExpression;
import org.renameme.painless.node.SFor;
import org.renameme.painless.node.SFunction;
import org.renameme.painless.node.SIf;
import org.renameme.painless.node.SIfElse;
import org.renameme.painless.node.SReturn;
import org.renameme.painless.node.SThrow;
import org.renameme.painless.node.STry;
import org.renameme.painless.node.SWhile;

public class UserTreeBaseVisitor<Scope> implements UserTreeVisitor<Scope> {

    @Override
    public void visitClass(SClass userClassNode, Scope scope) {
        userClassNode.visitChildren(this, scope);
    }

    @Override
    public void visitFunction(SFunction userClassNode, Scope scope) {
        userClassNode.visitChildren(this, scope);
    }

    @Override
    public void visitBlock(SBlock userBlockNode, Scope scope) {
        userBlockNode.visitChildren(this, scope);
    }

    @Override
    public void visitIf(SIf userIfNode, Scope scope) {
        userIfNode.visitChildren(this, scope);
    }

    @Override
    public void visitIfElse(SIfElse userIfElseNode, Scope scope) {
        userIfElseNode.visitChildren(this, scope);
    }

    @Override
    public void visitWhile(SWhile userWhileNode, Scope scope) {
        userWhileNode.visitChildren(this, scope);
    }

    @Override
    public void visitDo(SDo userDoNode, Scope scope) {
        userDoNode.visitChildren(this, scope);
    }

    @Override
    public void visitFor(SFor userForNode, Scope scope) {
        userForNode.visitChildren(this, scope);
    }

    @Override
    public void visitEach(SEach userEachNode, Scope scope) {
        userEachNode.visitChildren(this, scope);
    }

    @Override
    public void visitDeclBlock(SDeclBlock userDeclBlockNode, Scope scope) {
        userDeclBlockNode.visitChildren(this, scope);
    }

    @Override
    public void visitDeclaration(SDeclaration userDeclarationNode, Scope scope) {
        userDeclarationNode.visitChildren(this, scope);
    }

    @Override
    public void visitReturn(SReturn userReturnNode, Scope scope) {
        userReturnNode.visitChildren(this, scope);
    }

    @Override
    public void visitExpression(SExpression userExpressionNode, Scope scope) {
        userExpressionNode.visitChildren(this, scope);
    }

    @Override
    public void visitTry(STry userTryNode, Scope scope) {
        userTryNode.visitChildren(this, scope);
    }

    @Override
    public void visitCatch(SCatch userCatchNode, Scope scope) {
        userCatchNode.visitChildren(this, scope);
    }

    @Override
    public void visitThrow(SThrow userThrowNode, Scope scope) {
        userThrowNode.visitChildren(this, scope);
    }

    @Override
    public void visitContinue(SContinue userContinueNode, Scope scope) {
        userContinueNode.visitChildren(this, scope);
    }

    @Override
    public void visitBreak(SBreak userBreakNode, Scope scope) {
        userBreakNode.visitChildren(this, scope);
    }

    @Override
    public void visitAssignment(EAssignment userAssignmentNode, Scope scope) {
        userAssignmentNode.visitChildren(this, scope);
    }

    @Override
    public void visitUnary(EUnary userUnaryNode, Scope scope) {
        userUnaryNode.visitChildren(this, scope);
    }

    @Override
    public void visitBinary(EBinary userBinaryNode, Scope scope) {
        userBinaryNode.visitChildren(this, scope);
    }

    @Override
    public void visitBooleanComp(EBooleanComp userBooleanCompNode, Scope scope) {
        userBooleanCompNode.visitChildren(this, scope);
    }

    @Override
    public void visitComp(EComp userCompNode, Scope scope) {
        userCompNode.visitChildren(this, scope);
    }

    @Override
    public void visitExplicit(EExplicit userExplicitNode, Scope scope) {
        userExplicitNode.visitChildren(this, scope);
    }

    @Override
    public void visitInstanceof(EInstanceof userInstanceofNode, Scope scope) {
        userInstanceofNode.visitChildren(this, scope);
    }

    @Override
    public void visitConditional(EConditional userConditionalNode, Scope scope) {
        userConditionalNode.visitChildren(this, scope);
    }

    @Override
    public void visitElvis(EElvis userElvisNode, Scope scope) {
        userElvisNode.visitChildren(this, scope);
    }

    @Override
    public void visitListInit(EListInit userListInitNode, Scope scope) {
        userListInitNode.visitChildren(this, scope);
    }

    @Override
    public void visitMapInit(EMapInit userMapInitNode, Scope scope) {
        userMapInitNode.visitChildren(this, scope);
    }

    @Override
    public void visitNewArray(ENewArray userNewArrayNode, Scope scope) {
        userNewArrayNode.visitChildren(this, scope);
    }

    @Override
    public void visitNewObj(ENewObj userNewObjNode, Scope scope) {
        userNewObjNode.visitChildren(this, scope);
    }

    @Override
    public void visitCallLocal(ECallLocal userCallLocalNode, Scope scope) {
        userCallLocalNode.visitChildren(this, scope);
    }

    @Override
    public void visitBooleanConstant(EBooleanConstant userBooleanConstantNode, Scope scope) {
        userBooleanConstantNode.visitChildren(this, scope);
    }

    @Override
    public void visitNumeric(ENumeric userNumericNode, Scope scope) {
        userNumericNode.visitChildren(this, scope);
    }

    @Override
    public void visitDecimal(EDecimal userDecimalNode, Scope scope) {
        userDecimalNode.visitChildren(this, scope);
    }

    @Override
    public void visitString(EString userStringNode, Scope scope) {
        userStringNode.visitChildren(this, scope);
    }

    @Override
    public void visitNull(ENull userNullNode, Scope scope) {
        userNullNode.visitChildren(this, scope);
    }

    @Override
    public void visitRegex(ERegex userRegexNode, Scope scope) {
        userRegexNode.visitChildren(this, scope);
    }

    @Override
    public void visitLambda(ELambda userLambdaNode, Scope scope) {
        userLambdaNode.visitChildren(this, scope);
    }

    @Override
    public void visitFunctionRef(EFunctionRef userFunctionRefNode, Scope scope) {
        userFunctionRefNode.visitChildren(this, scope);
    }

    @Override
    public void visitNewArrayFunctionRef(ENewArrayFunctionRef userNewArrayFunctionRefNode, Scope scope) {
        userNewArrayFunctionRefNode.visitChildren(this, scope);
    }

    @Override
    public void visitSymbol(ESymbol userSymbolNode, Scope scope) {
        userSymbolNode.visitChildren(this, scope);
    }

    @Override
    public void visitDot(EDot userDotNode, Scope scope) {
        userDotNode.visitChildren(this, scope);
    }

    @Override
    public void visitBrace(EBrace userBraceNode, Scope scope) {
        userBraceNode.visitChildren(this, scope);
    }

    @Override
    public void visitCall(ECall userCallNode, Scope scope) {
        userCallNode.visitChildren(this, scope);
    }
}
