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

public interface UserTreeVisitor<Scope> {

    void visitClass(SClass userClassNode, Scope scope);
    void visitFunction(SFunction userFunctionNode, Scope scope);

    void visitBlock(SBlock userBlockNode, Scope scope);
    void visitIf(SIf userIfNode, Scope scope);
    void visitIfElse(SIfElse userIfElseNode, Scope scope);
    void visitWhile(SWhile userWhileNode, Scope scope);
    void visitDo(SDo userDoNode, Scope scope);
    void visitFor(SFor userForNode, Scope scope);
    void visitEach(SEach userEachNode, Scope scope);
    void visitDeclBlock(SDeclBlock userDeclBlockNode, Scope scope);
    void visitDeclaration(SDeclaration userDeclarationNode, Scope scope);
    void visitReturn(SReturn userReturnNode, Scope scope);
    void visitExpression(SExpression userExpressionNode, Scope scope);
    void visitTry(STry userTryNode, Scope scope);
    void visitCatch(SCatch userCatchNode, Scope scope);
    void visitThrow(SThrow userThrowNode, Scope scope);
    void visitContinue(SContinue userContinueNode, Scope scope);
    void visitBreak(SBreak userBreakNode, Scope scope);

    void visitAssignment(EAssignment userAssignmentNode, Scope scope);
    void visitUnary(EUnary userUnaryNode, Scope scope);
    void visitBinary(EBinary userBinaryNode, Scope scope);
    void visitBooleanComp(EBooleanComp userBooleanCompNode, Scope scope);
    void visitComp(EComp userCompNode, Scope scope);
    void visitExplicit(EExplicit userExplicitNode, Scope scope);
    void visitInstanceof(EInstanceof userInstanceofNode, Scope scope);
    void visitConditional(EConditional userConditionalNode, Scope scope);
    void visitElvis(EElvis userElvisNode, Scope scope);
    void visitListInit(EListInit userListInitNode, Scope scope);
    void visitMapInit(EMapInit userMapInitNode, Scope scope);
    void visitNewArray(ENewArray userNewArrayNode, Scope scope);
    void visitNewObj(ENewObj userNewObjectNode, Scope scope);
    void visitCallLocal(ECallLocal userCallLocalNode, Scope scope);
    void visitBooleanConstant(EBooleanConstant userBooleanConstantNode, Scope scope);
    void visitNumeric(ENumeric userNumericNode, Scope scope);
    void visitDecimal(EDecimal userDecimalNode, Scope scope);
    void visitString(EString userStringNode, Scope scope);
    void visitNull(ENull userNullNode, Scope scope);
    void visitRegex(ERegex userRegexNode, Scope scope);
    void visitLambda(ELambda userLambdaNode, Scope scope);
    void visitFunctionRef(EFunctionRef userFunctionRefNode, Scope scope);
    void visitNewArrayFunctionRef(ENewArrayFunctionRef userNewArrayFunctionRefNode, Scope scope);
    void visitSymbol(ESymbol userSymbolNode, Scope scope);
    void visitDot(EDot userDotNode, Scope scope);
    void visitBrace(EBrace userBraceNode, Scope scope);
    void visitCall(ECall userCallNode, Scope scope);
}
