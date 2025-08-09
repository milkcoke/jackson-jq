package net.thisptr.jackson.jq.internal.tree.binaryop;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import net.thisptr.jackson.jq.Expression;
import net.thisptr.jackson.jq.Version;
import net.thisptr.jackson.jq.internal.tree.binaryop.BinaryOperatorExpression.Operator.Associativity;
import net.thisptr.jackson.jq.internal.tree.binaryop.assignment.Assignment;
import net.thisptr.jackson.jq.internal.tree.binaryop.assignment.ComplexAlternativeAssignment;
import net.thisptr.jackson.jq.internal.tree.binaryop.assignment.ComplexDivideAssignment;
import net.thisptr.jackson.jq.internal.tree.binaryop.assignment.ComplexMinusAssignment;
import net.thisptr.jackson.jq.internal.tree.binaryop.assignment.ComplexModuloAssignment;
import net.thisptr.jackson.jq.internal.tree.binaryop.assignment.ComplexMultiplyAssignment;
import net.thisptr.jackson.jq.internal.tree.binaryop.assignment.ComplexPlusAssignment;
import net.thisptr.jackson.jq.internal.tree.binaryop.assignment.UpdateAssignment;
import net.thisptr.jackson.jq.internal.tree.binaryop.comparison.CompareEqualTest;
import net.thisptr.jackson.jq.internal.tree.binaryop.comparison.CompareGreaterEqualTest;
import net.thisptr.jackson.jq.internal.tree.binaryop.comparison.CompareGreaterTest;
import net.thisptr.jackson.jq.internal.tree.binaryop.comparison.CompareLessEqualTest;
import net.thisptr.jackson.jq.internal.tree.binaryop.comparison.CompareLessTest;
import net.thisptr.jackson.jq.internal.tree.binaryop.comparison.CompareNotEqualTest;

public abstract class BinaryOperatorExpression implements Expression {
	protected Expression lhs;
	protected Expression rhs;
	private String image;

	public BinaryOperatorExpression(final Expression lhs, final Expression rhs, final String image) {
		this.lhs = lhs;
		this.rhs = rhs;
		this.image = image;
	}

	@Override
	public String toString() {
		return String.format("(%s %s %s)", lhs, image, rhs);
	}

	public enum Operator {
		ASSIGN("=", 6, Associativity.RIGHT),
		UDPATE("|=", 6, Associativity.RIGHT),
		DEFAULT_EQUAL("//=", 6, Associativity.RIGHT),
		PLUS_EQUAL("+=", 6, Associativity.RIGHT),
		MINUS_EQUAL("-=", 6, Associativity.RIGHT),
		TIMES_EQUAL("*=", 6, Associativity.RIGHT),
		DIVIDE_EQUAL("/=", 6, Associativity.RIGHT),
		MODULO_EQUAL("%=", 6, Associativity.RIGHT),
		DEFAULT("//", 5, Associativity.LEFT),
		OR("or", 4, Associativity.LEFT),
		AND("and", 4, Associativity.LEFT),
		LESS_EQUAL("<=", 3, Associativity.LEFT),
		LESS("<", 3, Associativity.LEFT),
		GREATER_EQUAL(">=", 3, Associativity.LEFT),
		GREATER(">", 3, Associativity.LEFT),
		EQUAL("==", 3, Associativity.LEFT),
		NOT_EQUAL("!=", 3, Associativity.LEFT),
		PLUS("+", 2, Associativity.LEFT),
		MINUS("-", 2, Associativity.LEFT),
		MODULO("%", 1, Associativity.LEFT),
		DIVIDE("/", 1, Associativity.LEFT),
		TIMES("*", 1, Associativity.LEFT);

		public final String image;
		public final int precedence;
		public final Associativity associativity;

		public enum Associativity {
			LEFT, RIGHT
		}

		private Operator(final String image, final int precedence, final Associativity associativity) {
			this.image = image;
			this.precedence = precedence;
			this.associativity = associativity;
		}

		public static Operator fromImage(final String image) {
			final Operator op = lookup.get(image);
			if (op == null)
				throw new IllegalArgumentException();
			return op;
		}

		private static final Map<String, Operator> lookup = new HashMap<>();
		static {
			for (final Operator op : Operator.values())
				lookup.put(op.image, op);
		}

		public Expression buildTree(final Expression lhs, final Expression rhs, final Version version) {
			try {
                switch (this) {
                    case ASSIGN:
                        return new Assignment(lhs, rhs);
                    case UDPATE:
                        return new UpdateAssignment(lhs, rhs, version);
                    case DEFAULT_EQUAL:
                        return new ComplexAlternativeAssignment(lhs, rhs);
                    case PLUS_EQUAL:
                        return new ComplexPlusAssignment(lhs, rhs);
                    case MINUS_EQUAL:
                        return new ComplexMinusAssignment(lhs, rhs);
                    case TIMES_EQUAL:
                        return new ComplexMultiplyAssignment(lhs, rhs);
                    case DIVIDE_EQUAL:
                        return new ComplexDivideAssignment(lhs, rhs);
                    case MODULO_EQUAL:
                        return new ComplexModuloAssignment(lhs, rhs);
                    case DEFAULT:
                        return new AlternativeOperatorExpression(lhs, rhs);
                    case OR:
                        return new BooleanOrExpression(lhs, rhs);
                    case AND:
                        return new BooleanAndExpression(lhs, rhs);
                    case LESS_EQUAL:
                        return new CompareLessEqualTest(lhs, rhs);
                    case LESS:
                        return new CompareLessTest(lhs, rhs);
                    case GREATER_EQUAL:
                        return new CompareGreaterEqualTest(lhs, rhs);
                    case GREATER:
                        return new CompareGreaterTest(lhs, rhs);
                    case EQUAL:
                        return new CompareEqualTest(lhs, rhs);
                    case NOT_EQUAL:
                        return new CompareNotEqualTest(lhs, rhs);
                    case PLUS:
                        return new PlusExpression(lhs, rhs);
                    case MINUS:
                        return new MinusExpression(lhs, rhs);
                    case MODULO:
                        return new ModuloExpression(lhs, rhs);
                    case DIVIDE:
                        return new DivideExpression(lhs, rhs);
                    case TIMES:
                        return new MultiplyExpression(lhs, rhs);
                    default:
                        throw new IllegalStateException("Unknown operator: " + this);
                }
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static Expression buildTree(final List<Expression> exprs, final List<Operator> operators, final Version version) {
		if (exprs.size() != operators.size() + 1)
			throw new IllegalArgumentException();

		// shunting-yard algorithm
		final Stack<Expression> stackExprs = new Stack<>();
		final Stack<Operator> stackOperators = new Stack<>();

		final Iterator<Expression> iterExpr = exprs.iterator();
		final Iterator<Operator> iterOperator = operators.iterator();

		stackExprs.push(iterExpr.next());
		while (iterExpr.hasNext()) {
			final Operator op1 = iterOperator.next();
			while (!stackOperators.isEmpty()) {
				final Operator op2 = stackOperators.peek();
				if (op1.precedence > op2.precedence
						|| op1.precedence == op2.precedence && op1.associativity == Associativity.LEFT) {
					final Operator op = stackOperators.pop();
					final Expression rhs = stackExprs.pop();
					final Expression lhs = stackExprs.pop();
					stackExprs.push(op.buildTree(lhs, rhs, version));
				} else {
					break;
				}
			}
			stackOperators.push(op1);
			stackExprs.push(iterExpr.next());
		}

		while (!stackOperators.isEmpty()) {
			final Operator op = stackOperators.pop();
			final Expression rhs = stackExprs.pop();
			final Expression lhs = stackExprs.pop();
			stackExprs.push(op.buildTree(lhs, rhs, version));
		}

		return stackExprs.get(0);
	}
}
