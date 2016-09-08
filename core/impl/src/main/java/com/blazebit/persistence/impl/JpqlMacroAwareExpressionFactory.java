package com.blazebit.persistence.impl;

import com.blazebit.persistence.impl.expression.Expression;
import com.blazebit.persistence.impl.expression.ExpressionFactory;
import com.blazebit.persistence.impl.expression.MacroConfiguration;
import com.blazebit.persistence.impl.expression.PathExpression;
import com.blazebit.persistence.impl.predicate.Predicate;

import java.util.List;

/**
 *
 * @author Christian Beikov
 * @since 1.2.0
 */
public final class JpqlMacroAwareExpressionFactory implements ExpressionFactory {

    private final ExpressionFactory expressionFactory;
    private final JpqlMacroStorage macroStorage;

    public JpqlMacroAwareExpressionFactory(ExpressionFactory expressionFactory, JpqlMacroStorage macroStorage) {
        this.expressionFactory = expressionFactory;
        this.macroStorage = macroStorage;
    }

    @Override
    public <T extends ExpressionFactory> T unwrap(Class<T> clazz) {
        if (JpqlMacroAwareExpressionFactory.class.isAssignableFrom(clazz)) {
            return (T) this;
        }
        return expressionFactory.unwrap(clazz);
    }

    @Override
    public PathExpression createPathExpression(String expression) {
        return expressionFactory.createPathExpression(expression, macroStorage.getMacroConfiguration());
    }

    @Override
    public PathExpression createPathExpression(String expression, MacroConfiguration macroConfiguration) {
        return expressionFactory.createPathExpression(expression, macroConfiguration);
    }

    @Override
    public Expression createJoinPathExpression(String expression) {
        return expressionFactory.createJoinPathExpression(expression, macroStorage.getMacroConfiguration());
    }

    @Override
    public Expression createJoinPathExpression(String expression, MacroConfiguration macroConfiguration) {
        return expressionFactory.createJoinPathExpression(expression, macroConfiguration);
    }

    @Override
    public Expression createSimpleExpression(String expression, boolean allowQuantifiedPredicates) {
        return expressionFactory.createSimpleExpression(expression, allowQuantifiedPredicates, macroStorage.getMacroConfiguration());
    }

    @Override
    public Expression createSimpleExpression(String expression, boolean allowQuantifiedPredicates, MacroConfiguration macroConfiguration) {
        return expressionFactory.createSimpleExpression(expression, allowQuantifiedPredicates, macroConfiguration);
    }

    @Override
    public Expression createCaseOperandExpression(String caseOperandExpression) {
        return expressionFactory.createCaseOperandExpression(caseOperandExpression, macroStorage.getMacroConfiguration());
    }

    @Override
    public Expression createCaseOperandExpression(String caseOperandExpression, MacroConfiguration macroConfiguration) {
        return expressionFactory.createCaseOperandExpression(caseOperandExpression, macroConfiguration);
    }

    @Override
    public Expression createScalarExpression(String expression) {
        return expressionFactory.createScalarExpression(expression, macroStorage.getMacroConfiguration());
    }

    @Override
    public Expression createScalarExpression(String expression, MacroConfiguration macroConfiguration) {
        return expressionFactory.createScalarExpression(expression, macroConfiguration);
    }

    @Override
    public Expression createArithmeticExpression(String expression) {
        return expressionFactory.createArithmeticExpression(expression, macroStorage.getMacroConfiguration());
    }

    @Override
    public Expression createArithmeticExpression(String expression, MacroConfiguration macroConfiguration) {
        return expressionFactory.createArithmeticExpression(expression, macroConfiguration);
    }

    @Override
    public Expression createStringExpression(String expression) {
        return expressionFactory.createStringExpression(expression, macroStorage.getMacroConfiguration());
    }

    @Override
    public Expression createStringExpression(String expression, MacroConfiguration macroConfiguration) {
        return expressionFactory.createStringExpression(expression, macroConfiguration);
    }

    @Override
    public Expression createOrderByExpression(String expression) {
        return expressionFactory.createOrderByExpression(expression, macroStorage.getMacroConfiguration());
    }

    @Override
    public Expression createOrderByExpression(String expression, MacroConfiguration macroConfiguration) {
        return expressionFactory.createOrderByExpression(expression, macroConfiguration);
    }

    @Override
    public List<Expression> createInItemExpressions(String[] parameterOrLiteralExpressions) {
        return expressionFactory.createInItemExpressions(parameterOrLiteralExpressions, macroStorage.getMacroConfiguration());
    }

    @Override
    public List<Expression> createInItemExpressions(String[] parameterOrLiteralExpressions, MacroConfiguration macroConfiguration) {
        return expressionFactory.createInItemExpressions(parameterOrLiteralExpressions, macroConfiguration);
    }

    @Override
    public Expression createInItemExpression(String parameterOrLiteralExpression) {
        return expressionFactory.createInItemExpression(parameterOrLiteralExpression, macroStorage.getMacroConfiguration());
    }

    @Override
    public Expression createInItemExpression(String parameterOrLiteralExpression, MacroConfiguration macroConfiguration) {
        return expressionFactory.createInItemExpression(parameterOrLiteralExpression, macroConfiguration);
    }

    @Override
    public Predicate createBooleanExpression(String expression, boolean allowQuantifiedPredicates) {
        return expressionFactory.createBooleanExpression(expression, allowQuantifiedPredicates, macroStorage.getMacroConfiguration());
    }

    @Override
    public Predicate createBooleanExpression(String expression, boolean allowQuantifiedPredicates, MacroConfiguration macroConfiguration) {
        return expressionFactory.createBooleanExpression(expression, allowQuantifiedPredicates, macroConfiguration);
    }
}