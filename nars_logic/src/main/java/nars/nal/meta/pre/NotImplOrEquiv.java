package nars.nal.meta.pre;

import nars.Op;
import nars.nal.RuleMatch;
import nars.term.Term;


public final class NotImplOrEquiv extends PreCondition1 {

    public NotImplOrEquiv(Term arg1) {
        super(arg1);
    }

    @Override
    public boolean test(RuleMatch m, Term arg1) {

        if (arg1 == null) return false;

        //TODO use a bitvector to test Op membership in this set
        //  and then abstract this to a generic Precondition
        //  that can be used for allowing (+) or denying (-)
        //  other sets of Ops

        Op o = arg1.op();
        switch (o) {
            case IMPLICATION:
            case IMPLICATION_AFTER:
            case IMPLICATION_BEFORE:
            case IMPLICATION_WHEN:
            case EQUIV:
            case EQUIV_AFTER:
            case EQUIV_WHEN:
                return false;
        }
        return true;
    }

}
