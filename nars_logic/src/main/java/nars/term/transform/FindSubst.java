package nars.term.transform;

import nars.Global;
import nars.Op;
import nars.nal.nal4.Image;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Variable;
import nars.term.Variables;

import java.util.Map;
import java.util.Random;

/**
 * Created by me on 8/17/15.
 */
public class FindSubst {
    private final Op type;
    public final Map<Term, Term> map1;
    public final Map<Term, Term> map2;
    private final Random random;

    public FindSubst(Op type, Random random) {
        this(type, null, null, random);
    }

    public FindSubst(Op type, Map map1, Map map2, Random random) {
        if (map1 == null)
            map1 = Global.newHashMap(0);
        if (map2 == null)
            map2 = Global.newHashMap(0);

        this.type = type;
        this.map1 = map1;
        this.map2 = map2;
        this.random = random;

    }

    public void clear() {
        map1.clear();
        map2.clear();
    }


    @Override
    public String toString() {
        return type + ":" + map1 + "," + map2;
    }

    private void print(String prefix, Term a, Term b) {
        System.out.print(prefix);
        if (a != null)
            System.out.println(" " + a + " ||| " + b);
        else
            System.out.println();
        System.out.println("     " + this);
    }

    /**
     * recursess into the next sublevel of the term
     */
    public boolean next(final Term term1, final Term term2) {

        final Variable term1Var = term1 instanceof Variable ? (Variable) term1 : null;
        final Variable term2Var = term2 instanceof Variable ? (Variable) term2 : null;

        final boolean termsEqual = term1.equals(term2);
        if (term1Var != null && term2Var != null && termsEqual) {
            return true;
        }

        if (term1Var != null && term1Var.op == type) {

            final Term t = map1.get(term1Var);

            if (t != null) {
                return next(t, term2);
            }
            else {
                if ((term2Var != null) && (term2Var.op == type)) {
                    putCommon(term1Var, term2Var);
                } else {

                    if ((term2Var!=null) && !queryVarMatch(term1Var, term2Var))
                        return false;

                    put1To2(term2, term1Var);
                }

                return true;
            }

        } else if (term2Var != null && term2Var.op == type) {

            final Term t = map2.get(term2Var);

            if (t != null)
                return next(term1, t);
            else {
                put2To1(term1, term2Var);
                return true;
            }

        } else if ((term1 instanceof Compound) && ((term1.op() == term2.op())) && (term1.hasVar(type) || term2.hasVar(type))) {
            final Compound cTerm1 = (Compound) term1;
            final Compound cTerm2 = (Compound) term2;
            final int c1Len = cTerm1.length();
            if (c1Len != cTerm2.length()) {
                return false;
            }
            //TODO simplify comparison with Image base class
            if ((cTerm1 instanceof Image) && (((Image) cTerm1).relationIndex != ((Image) cTerm2).relationIndex)) {
                return false;
            }

            final Term[] list;
            if (cTerm1.isCommutative() && c1Len > 1) {
                if (c1Len == 2) {
                    return permute2(cTerm1.term(0), cTerm1.term(1), cTerm2);
                } else if (c1Len ==3) {
                    return permute3(cTerm1.term, cTerm2);
                } else {
                    list = cTerm1.cloneTerms();
                    Compound.shuffle(list, random);
                }
            } else {
                list = cTerm1.term;
            }

            return next(cTerm2, list);
        }

        return termsEqual;
    }


    /** //https://github.com/opennars/opennars/commit/dd70cb81d22ad968ece86a549057cd19aad8bff3 */
    protected boolean queryVarMatch(Variable term1Var, Variable term2Var) {

        final boolean t1Query = (term1Var.op == Op.VAR_QUERY);
        final boolean t2Query = (term2Var.op == Op.VAR_QUERY);

        return (t1Query ^ t2Query);
    }

    /** elimination */
    private final void put2To1(final Term term1, final Variable term2Var) {
        if (term2Var instanceof Variables.CommonVariable) {
            map1.put(term2Var, term1);
        }
        map2.put(term2Var, term1);
    }

    /** elimination */
    private final void put1To2(final Term term2, final Variable term1Var) {
        map1.put(term1Var, term2);
        if (term1Var instanceof Variables.CommonVariable) {
            map2.put(term1Var, term2);
        }
    }

    /**
     * unification.
     *
     * override this to disable common variables.
     * for example, it may be required to default to variable a
     * (Term 1 if it is a variable and of the target type)
     * instead of a new common variable.
     * */
    protected void putCommon(Variable a, Variable b) {
        Variable commonVar = Variables.CommonVariable.make(a, b);
        map1.put(a, commonVar);
        map2.put(b, commonVar);
    }

    private boolean permute3(final Term[] c3, final Compound cTerm2) {
        int order = random.nextInt(6);
        final Term a = c3[0];
        final Term b = c3[1];
        final Term c = c3[2];
        final int maxTries = 6;
        int tries = 0;
        final Term[] list = new Term[3];
        boolean solved;
        do {
            switch (order) {
                case 0: list[0] = a; list[1] = b; list[2] = c; break;
                case 1: list[0] = a; list[1] = c; list[2] = b; break;
                case 2: list[0] = b; list[1] = a; list[2] = c; break;
                case 3: list[0] = b; list[1] = c; list[2] = a; break;
                case 4: list[0] = c; list[1] = a; list[2] = b; break;
                case 5: list[0] = c; list[1] = b; list[2] = a; break;
            }
            solved = next(cTerm2, list);
            order = (order + 1) % 6;
            tries++;
        } while (tries < maxTries && !solved);
        /*if (solved && tries > 1) {
            System.out.println("solved true after " + tries);
        }*/
        return solved;
    }

    private boolean permute2(final Term cTerm1_0, final Term cTerm1_1, final Compound cTerm2) {
        Term[] list = new Term[2];
        boolean order = random.nextBoolean();
        int tries = 0;
        boolean solved;
        do {
            if (order) {
                list[0] = cTerm1_0;
                list[1] = cTerm1_1;
            } else {
                list[0] = cTerm1_1;
                list[1] = cTerm1_0;
            }
            order = !order;
            solved = next(cTerm2, list);
            tries++;
        } while (tries < 2 && !solved);

//   if (solved) {
//      if (tries > 1)
    //   System.out.println("got it " + tries);
//   }

        return solved;
    }

    /**
     * a branch for comparing a particular permutation, called from the main next()
     */
    final protected boolean next(final Compound x, final Term[] t) {
        final Term X[] = x.term;
        final int tlen = t.length;
        for (int i = 0; i < tlen; i++) {
            if (!next(t[i], X[i])) {
                return false;
            }
        }
        return true;
    }


}
