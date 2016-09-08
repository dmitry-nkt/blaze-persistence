package com.blazebit.persistence.impl;

import com.blazebit.persistence.impl.expression.Expression;
import com.blazebit.persistence.impl.expression.ExpressionFactory;
import com.blazebit.persistence.impl.expression.MacroFunction;
import com.blazebit.persistence.spi.JpqlMacro;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Christian Beikov
 * @since 1.2.0
 */
public class JpqlMacroAdapter implements MacroFunction {

    private final JpqlMacro macro;
    private final ExpressionFactory expressionFactory;

    public JpqlMacroAdapter(JpqlMacro macro, ExpressionFactory expressionFactory) {
        this.macro = macro;
        this.expressionFactory = expressionFactory;
    }

    public static Map<String, MacroFunction> createMacros(Map<String, JpqlMacro> jpqlMacros, ExpressionFactory expressionFactory) {
        Map<String, MacroFunction> map = new HashMap<String, MacroFunction>(jpqlMacros.size());
        for (Map.Entry<String, JpqlMacro> entry : jpqlMacros.entrySet()) {
            map.put(entry.getKey(), new JpqlMacroAdapter(entry.getValue(), expressionFactory));
        }
        return map;
    }

    @Override
    public Expression apply(List<Expression> expressions) {
        JpqlMacroFunctionRenderContext context = new JpqlMacroFunctionRenderContext(expressions);
        macro.render(context);
        return expressionFactory.createSimpleExpression(context.renderToString(), false);
    }

}