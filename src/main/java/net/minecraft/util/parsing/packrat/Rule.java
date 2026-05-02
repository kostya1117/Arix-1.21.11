package net.minecraft.util.parsing.packrat;

import org.jspecify.annotations.Nullable;

public interface Rule<S, T> {
     T parse(ParseState<S> p_335539_);

    static <S, T> Rule<S, T> fromTerm(Term<S> p_334127_, Rule.RuleAction<S, T> p_334890_) {
        return new Rule.WrappedTerm<>(p_334890_, p_334127_);
    }

    static <S, T> Rule<S, T> fromTerm(Term<S> p_336211_, Rule.SimpleRuleAction<S, T> p_332994_) {
        return new Rule.WrappedTerm<>(p_332994_, p_336211_);
    }

    @FunctionalInterface
    interface RuleAction<S, T> {
         T run(ParseState<S> p_332162_);
    }

    @FunctionalInterface
    interface SimpleRuleAction<S, T> extends Rule.RuleAction<S, T> {
        T run(Scope p_332535_);

        @Override
        default T run(ParseState<S> p_392774_) {
            return this.run(p_392774_.scope());
        }
    }

    record WrappedTerm<S, T>(Rule.RuleAction<S, T> action, Term<S> child) implements Rule<S, T> {
        @Override
        public  T parse(ParseState<S> p_328860_) {
            Scope scope = p_328860_.scope();
            scope.pushFrame();

            try {
                return this.child.parse(p_328860_, scope, Control.UNBOUND) ? this.action.run(p_328860_) : null;
            } finally {
                scope.popFrame();
            }
        }
    }
}
