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

package org.renameme.painless.symbol;

import org.renameme.painless.FunctionRef;
import org.renameme.painless.ir.IRNode;
import org.renameme.painless.lookup.PainlessCast;
import org.renameme.painless.lookup.PainlessClassBinding;
import org.renameme.painless.lookup.PainlessConstructor;
import org.renameme.painless.lookup.PainlessField;
import org.renameme.painless.lookup.PainlessInstanceBinding;
import org.renameme.painless.lookup.PainlessLookupUtility;
import org.renameme.painless.lookup.PainlessMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Decorations {

    // standard input for user statement nodes during semantic phase

    public interface LastSource extends Decorator.Condition {

    }

    public interface BeginLoop extends Decorator.Condition {

    }

    public interface InLoop extends Decorator.Condition {

    }

    public interface LastLoop extends Decorator.Condition {

    }

    // standard output for user statement nodes during semantic phase

    public interface MethodEscape extends Decorator.Condition {

    }

    public interface LoopEscape extends Decorator.Condition {

    }

    public interface AllEscape extends Decorator.Condition {

    }

    public interface AnyContinue extends Decorator.Condition {

    }

    public interface AnyBreak extends Decorator.Condition {

    }

    // standard input for user expression nodes during semantic phase

    public interface Read extends Decorator.Condition {

    }

    public interface Write extends Decorator.Condition {

    }

    public static class TargetType implements Decorator.Decoration {

        private final Class<?> targetType;

        public TargetType(Class<?> targetType) {
            this.targetType = Objects.requireNonNull(targetType);
        }

        public Class<?> getTargetType() {
            return targetType;
        }

        public String getTargetCanonicalTypeName() {
            return PainlessLookupUtility.typeToCanonicalTypeName(targetType);
        }
    }

    public interface Explicit extends Decorator.Condition {

    }

    public interface Internal extends Decorator.Condition {

    }

    // standard output for user expression node during semantic phase

    public static class ValueType implements Decorator.Decoration {

        private final Class<?> valueType;

        public ValueType(Class<?> valueType) {
            this.valueType = Objects.requireNonNull(valueType);
        }

        public Class<?> getValueType() {
            return valueType;
        }

        public String getValueCanonicalTypeName() {
            return PainlessLookupUtility.typeToCanonicalTypeName(valueType);
        }
    }

    public static class StaticType implements Decorator.Decoration {

        private final Class<?> staticType;

        public StaticType(Class<?> staticType) {
            this.staticType = Objects.requireNonNull(staticType);
        }

        public Class<?> getStaticType() {
            return staticType;
        }

        public String getStaticCanonicalTypeName() {
            return PainlessLookupUtility.typeToCanonicalTypeName(staticType);
        }
    }

    public static class PartialCanonicalTypeName implements Decorator.Decoration {

        private final String partialCanonicalTypeName;

        public PartialCanonicalTypeName(String partialCanonicalTypeName) {
            this.partialCanonicalTypeName = Objects.requireNonNull(partialCanonicalTypeName);
        }

        public String getPartialCanonicalTypeName() {
            return partialCanonicalTypeName;
        }
    }

    public interface DefOptimized extends Decorator.Condition {

    }

    // additional output acquired during the semantic process

    public interface ContinuousLoop extends Decorator.Condition {

    }

    public interface Shortcut extends Decorator.Condition {

    }

    public interface MapShortcut extends Decorator.Condition {

    }

    public interface ListShortcut extends Decorator.Condition {

    }

    public static class ExpressionPainlessCast implements Decorator.Decoration {

        private final PainlessCast expressionPainlessCast;

        public ExpressionPainlessCast(PainlessCast expressionPainlessCast) {
            this.expressionPainlessCast = Objects.requireNonNull(expressionPainlessCast);
        }

        public PainlessCast getExpressionPainlessCast() {
            return expressionPainlessCast;
        }
    }

    public static class SemanticVariable implements Decorator.Decoration {

        private final SemanticScope.Variable semanticVariable;

        public SemanticVariable(SemanticScope.Variable semanticVariable) {
            this.semanticVariable = semanticVariable;
        }

        public SemanticScope.Variable getSemanticVariable() {
            return semanticVariable;
        }
    }

    public static class IterablePainlessMethod implements Decorator.Decoration {

        private final PainlessMethod iterablePainlessMethod;

        public IterablePainlessMethod(PainlessMethod iterablePainlessMethod) {
            this.iterablePainlessMethod = Objects.requireNonNull(iterablePainlessMethod);
        }

        public PainlessMethod getIterablePainlessMethod() {
            return iterablePainlessMethod;
        }
    }

    public static class UnaryType implements Decorator.Decoration {

        private final Class<?> unaryType;

        public UnaryType(Class<?> unaryType) {
            this.unaryType = Objects.requireNonNull(unaryType);
        }

        public Class<?> getUnaryType() {
            return unaryType;
        }

        public String getUnaryCanonicalTypeName() {
            return PainlessLookupUtility.typeToCanonicalTypeName(unaryType);
        }
    }

    public static class BinaryType implements Decorator.Decoration {

        private final Class<?> binaryType;

        public BinaryType(Class<?> binaryType) {
            this.binaryType = Objects.requireNonNull(binaryType);
        }

        public Class<?> getBinaryType() {
            return binaryType;
        }

        public String getBinaryCanonicalTypeName() {
            return PainlessLookupUtility.typeToCanonicalTypeName(binaryType);
        }
    }

    public static class ShiftType implements Decorator.Decoration {

        private final Class<?> shiftType;

        public ShiftType(Class<?> shiftType) {
            this.shiftType = Objects.requireNonNull(shiftType);
        }

        public Class<?> getShiftType() {
            return shiftType;
        }

        public String getShiftCanonicalTypeName() {
            return PainlessLookupUtility.typeToCanonicalTypeName(shiftType);
        }
    }

    public static class ComparisonType implements Decorator.Decoration {

        private final Class<?> comparisonType;

        public ComparisonType(Class<?> comparisonType) {
            this.comparisonType = Objects.requireNonNull(comparisonType);
        }

        public Class<?> getComparisonType() {
            return comparisonType;
        }

        public String getComparisonCanonicalTypeName() {
            return PainlessLookupUtility.typeToCanonicalTypeName(comparisonType);
        }
    }

    public static class CompoundType implements Decorator.Decoration {

        private final Class<?> compoundType;

        public CompoundType(Class<?> compoundType) {
            this.compoundType = Objects.requireNonNull(compoundType);
        }

        public Class<?> getCompoundType() {
            return compoundType;
        }

        public String getCompoundCanonicalTypeName() {
            return PainlessLookupUtility.typeToCanonicalTypeName(compoundType);
        }
    }

    public static class UpcastPainlessCast implements Decorator.Decoration {

        private final PainlessCast upcastPainlessCast;

        public UpcastPainlessCast(PainlessCast upcastPainlessCast) {
            this.upcastPainlessCast = Objects.requireNonNull(upcastPainlessCast);
        }

        public PainlessCast getUpcastPainlessCast() {
            return upcastPainlessCast;
        }
    }

    public static class DowncastPainlessCast implements Decorator.Decoration {

        private final PainlessCast downcastPainlessCast;

        public DowncastPainlessCast(PainlessCast downcastPainlessCast) {
            this.downcastPainlessCast = Objects.requireNonNull(downcastPainlessCast);
        }

        public PainlessCast getDowncastPainlessCast() {
            return downcastPainlessCast;
        }
    }

    public static class StandardPainlessField implements Decorator.Decoration {

        private final PainlessField standardPainlessField;

        public StandardPainlessField(PainlessField standardPainlessField) {
            this.standardPainlessField = Objects.requireNonNull(standardPainlessField);
        }

        public PainlessField getStandardPainlessField() {
            return standardPainlessField;
        }
    }

    public static class StandardPainlessConstructor implements Decorator.Decoration {

        private final PainlessConstructor standardPainlessConstructor;

        public StandardPainlessConstructor(PainlessConstructor standardPainlessConstructor) {
            this.standardPainlessConstructor = Objects.requireNonNull(standardPainlessConstructor);
        }

        public PainlessConstructor getStandardPainlessConstructor() {
            return standardPainlessConstructor;
        }
    }

    public static class StandardPainlessMethod implements Decorator.Decoration {

        private final PainlessMethod standardPainlessMethod;

        public StandardPainlessMethod(PainlessMethod standardPainlessMethod) {
            this.standardPainlessMethod = Objects.requireNonNull(standardPainlessMethod);
        }

        public PainlessMethod getStandardPainlessMethod() {
            return standardPainlessMethod;
        }
    }

    public static class GetterPainlessMethod implements Decorator.Decoration {

        private final PainlessMethod getterPainlessMethod;

        public GetterPainlessMethod(PainlessMethod getterPainlessMethod) {
            this.getterPainlessMethod = Objects.requireNonNull(getterPainlessMethod);
        }

        public PainlessMethod getGetterPainlessMethod() {
            return getterPainlessMethod;
        }
    }

    public static class SetterPainlessMethod implements Decorator.Decoration {

        private final PainlessMethod setterPainlessMethod;

        public SetterPainlessMethod(PainlessMethod setterPainlessMethod) {
            this.setterPainlessMethod = Objects.requireNonNull(setterPainlessMethod);
        }

        public PainlessMethod getSetterPainlessMethod() {
            return setterPainlessMethod;
        }
    }

    public static class StandardConstant implements Decorator.Decoration {

        private final Object standardConstant;

        public StandardConstant(Object standardConstant) {
            this.standardConstant = Objects.requireNonNull(standardConstant);
        }

        public Object getStandardConstant() {
            return standardConstant;
        }
    }

    public static class StandardLocalFunction implements Decorator.Decoration {

        private final FunctionTable.LocalFunction localFunction;

        public StandardLocalFunction(FunctionTable.LocalFunction localFunction) {
            this.localFunction = Objects.requireNonNull(localFunction);
        }

        public FunctionTable.LocalFunction getLocalFunction() {
            return localFunction;
        }
    }

    public static class StandardPainlessClassBinding implements Decorator.Decoration {

        private final PainlessClassBinding painlessClassBinding;

        public StandardPainlessClassBinding(PainlessClassBinding painlessClassBinding) {
            this.painlessClassBinding = Objects.requireNonNull(painlessClassBinding);
        }

        public PainlessClassBinding getPainlessClassBinding() {
            return painlessClassBinding;
        }
    }

    public static class StandardPainlessInstanceBinding implements Decorator.Decoration {

        private final PainlessInstanceBinding painlessInstanceBinding;

        public StandardPainlessInstanceBinding(PainlessInstanceBinding painlessInstanceBinding) {
            this.painlessInstanceBinding = Objects.requireNonNull(painlessInstanceBinding);
        }

        public PainlessInstanceBinding getPainlessInstanceBinding() {
            return painlessInstanceBinding;
        }
    }

    public static class MethodNameDecoration implements Decorator.Decoration {

        private final String methodName;

        public MethodNameDecoration(String methodName) {
            this.methodName = Objects.requireNonNull(methodName);
        }

        public String getMethodName() {
            return methodName;
        }
    }

    public static class ReturnType implements Decorator.Decoration {

        private final Class<?> returnType;

        public ReturnType(Class<?> returnType) {
            this.returnType = Objects.requireNonNull(returnType);
        }

        public Class<?> getReturnType() {
            return returnType;
        }

        public String getReturnCanonicalTypeName() {
            return PainlessLookupUtility.typeToCanonicalTypeName(returnType);
        }
    }

    public static class TypeParameters implements Decorator.Decoration {

        private final List<Class<?>> typeParameters;

        public TypeParameters(List<Class<?>> typeParameters) {
            this.typeParameters = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(typeParameters)));
        }

        public List<Class<?>> getTypeParameters() {
            return typeParameters;
        }
    }

    public static class ParameterNames implements Decorator.Decoration {

        private final List<String> parameterNames;

        public ParameterNames(List<String> parameterNames) {
            this.parameterNames = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(parameterNames)));
        }

        public List<String> getParameterNames() {
            return parameterNames;
        }
    }

    public static class ReferenceDecoration implements Decorator.Decoration {

        private final FunctionRef reference;

        public ReferenceDecoration(FunctionRef reference) {
            this.reference = Objects.requireNonNull(reference);
        }

        public FunctionRef getReference() {
            return reference;
        }
    }

    public static class EncodingDecoration implements Decorator.Decoration {

        private final String encoding;

        public EncodingDecoration(String encoding) {
            this.encoding = Objects.requireNonNull(encoding);
        }

        public String getEncoding() {
            return encoding;
        }
    }

    public static class CapturesDecoration implements Decorator.Decoration {

        private final List<SemanticScope.Variable> captures;

        public CapturesDecoration(List<SemanticScope.Variable> captures) {
            this.captures = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(captures)));
        }

        public List<SemanticScope.Variable> getCaptures() {
            return captures;
        }
    }

    public static class InstanceType implements Decorator.Decoration {

        private final Class<?> instanceType;

        public InstanceType(Class<?> instanceType) {
            this.instanceType = Objects.requireNonNull(instanceType);
        }

        public Class<?> getInstanceType() {
            return instanceType;
        }

        public String getInstanceCanonicalTypeName() {
            return PainlessLookupUtility.typeToCanonicalTypeName(instanceType);
        }
    }

    public interface Negate extends Decorator.Condition {

    }

    public interface Compound extends Decorator.Condition {

    }

    public static class AccessDepth implements Decorator.Decoration {

        private final int accessDepth;

        public AccessDepth(int accessDepth) {
            this.accessDepth = accessDepth;
        }

        public int getAccessDepth() {
            return accessDepth;
        }
    }

    // standard output for user tree to ir tree phase

    public static class IRNodeDecoration implements Decorator.Decoration {

        private final IRNode irNode;

        public IRNodeDecoration(IRNode irNode) {
            this.irNode = Objects.requireNonNull(irNode);
        }

        public IRNode getIRNode() {
            return irNode;
        }
    }

    public static class Converter implements Decorator.Decoration {
        private final FunctionTable.LocalFunction converter;
        public Converter(FunctionTable.LocalFunction converter) {
            this.converter = converter;
        }

        public FunctionTable.LocalFunction getConverter() {
            return converter;
        }
    }

    // collect additional information about where doc is used

    public interface IsDocument extends Decorator.Condition {

    }
}
