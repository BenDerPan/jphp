package ru.regenix.jphp.syntax.generators;

import ru.regenix.jphp.common.Modifier;
import ru.regenix.jphp.tokenizer.TokenType;
import ru.regenix.jphp.tokenizer.token.Token;
import ru.regenix.jphp.tokenizer.token.expr.BraceExprToken;
import ru.regenix.jphp.tokenizer.token.expr.value.NameToken;
import ru.regenix.jphp.tokenizer.token.stmt.*;
import ru.regenix.jphp.syntax.SyntaxAnalyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class ClassGenerator extends Generator<ClassStmtToken> {

    @SuppressWarnings("unchecked")
    private final static Class<? extends Token>[] modifiers = new Class[]{
        PrivateStmtToken.class,
        ProtectedStmtToken.class,
        PublicStmtToken.class,
        StaticStmtToken.class,
        FinalStmtToken.class
    };

    public ClassGenerator(SyntaxAnalyzer analyzer) {
        super(analyzer);
    }

    protected void processName(ClassStmtToken result, ListIterator<Token> iterator){
        Token name = nextToken(iterator);
        if (name instanceof NameToken){
            result.setName((NameToken)name);
        } else
            unexpectedToken(name, TokenType.T_STRING);
    }

    protected void processExtends(ClassStmtToken result, ListIterator<Token> iterator){
        checkUnexpectedEnd(iterator);

        Token token = iterator.next();
        if (token instanceof ExtendsStmtToken){
            Token extend = analyzer.generateToken(token, iterator);
            result.setExtend((ExtendsStmtToken)extend);
        } else
            iterator.previous();
    }

    protected void processImplements(ClassStmtToken result, ListIterator<Token> iterator){
        checkUnexpectedEnd(iterator);
        Token token = iterator.next();
        if (token instanceof ImplementsStmtToken){
            Token implement = analyzer.generateToken(token, iterator);
            result.setImplement((ImplementsStmtToken) implement);
        } else
            iterator.previous();
    }

    @SuppressWarnings("unchecked")
    protected void processBody(ClassStmtToken result, ListIterator<Token> iterator){
        analyzer.setClazz(result);

        Token token = nextToken(iterator);
        if (token instanceof BraceExprToken){
            BraceExprToken brace = (BraceExprToken)token;
            if (brace.isBlockOpened()){

                List<ConstStmtToken> constants = new ArrayList<ConstStmtToken>();
                List<MethodStmtToken> methods = new ArrayList<MethodStmtToken>();
                List<Token> modifiers = new ArrayList<Token>();
                while (iterator.hasNext()){
                    Token current = iterator.next();
                    if (current instanceof ExprStmtToken)
                        unexpectedToken(current, "expression");

                    if (current instanceof ConstStmtToken){
                        ConstStmtToken one = analyzer.generator(ConstGenerator.class).getToken(current, iterator);
                        one.setClazz(result);
                        constants.add(one);
                        modifiers.clear();
                    } else if (isTokenClass(current, ClassGenerator.modifiers)){
                        for(Token modifier : modifiers){
                            if (modifier.getType() == current.getType())
                                unexpectedToken(current);
                        }
                        modifiers.add(current);
                    } else if (current instanceof FunctionStmtToken) {
                        FunctionStmtToken function = analyzer.generator(FunctionGenerator.class).getToken(current, iterator);
                        MethodStmtToken method = new MethodStmtToken(function);
                        method.setClazz(result);

                        for (Token modifier : modifiers){
                            if (modifier instanceof AbstractStmtToken)
                                method.setAbstract(true);
                            else if (modifier instanceof StaticStmtToken)
                                method.setStatic(true);
                            else if (modifier instanceof FinalStmtToken){
                                method.setFinal(true);
                            } else if (modifier instanceof PublicStmtToken){
                                if (method.getModifier() != null)
                                    unexpectedToken(modifier);

                                method.setModifier(Modifier.PUBLIC);
                            } else if (modifier instanceof PrivateStmtToken){
                                if (method.getModifier() != null)
                                    unexpectedToken(modifier);

                                method.setModifier(Modifier.PRIVATE);
                            } else if (modifier instanceof ProtectedStmtToken)  {
                                if (method.getModifier() != null)
                                    unexpectedToken(modifier);

                                method.setModifier(Modifier.PROTECTED);
                            }
                        }
                        if (method.getModifier() == null)
                            method.setModifier(Modifier.PUBLIC);

                        methods.add(method);
                        modifiers.clear();
                    } else if (isClosedBrace(current, BraceExprToken.Kind.BLOCK)){
                        break;
                    } else
                        unexpectedToken(current);
                }

                result.setConstants(constants);
                result.setMethods(methods);
                analyzer.setClazz(null);
                return;
            }
        }

        unexpectedToken(token, "{");
    }

    @SuppressWarnings("unchecked")
    protected ClassStmtToken processDefine(Token current, ListIterator<Token> iterator){
        if (isTokenClass(current, FinalStmtToken.class, AbstractStmtToken.class)){
            Token next = nextToken(iterator);
            if (next instanceof ClassStmtToken){
                ClassStmtToken result = (ClassStmtToken)next;
                result.setAbstract(current instanceof AbstractStmtToken);
                result.setFinal(current instanceof FinalStmtToken);
                result.setNamespace(analyzer.getNamespace());

                return result;
            } else {
                iterator.previous();
            }
        }

        if (current instanceof ClassStmtToken)
            return (ClassStmtToken)current;

        return null;
    }

    @Override
    public ClassStmtToken getToken(Token current, ListIterator<Token> iterator) {
        ClassStmtToken result = processDefine(current, iterator);

        if (result != null){
            if (analyzer.getClazz() != null)
                unexpectedToken(current);

            analyzer.setClazz(result);
            processName(result, iterator);
            processExtends(result, iterator);
            processImplements(result, iterator);
            processBody(result, iterator);
            analyzer.setClazz(null);
        }

        return result;
    }
}
