/*
 * Task.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.task;

import nars.Global;
import nars.Memory;
import nars.Symbols;
import nars.budget.Itemized;
import nars.nal.nal8.Operation;
import nars.task.stamp.Stamp;
import nars.term.Compound;
import nars.truth.ProjectedTruth;
import nars.truth.Truth;
import nars.truth.Truthed;

import javax.annotation.Nullable;
import java.lang.ref.Reference;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A task to be processed, consists of a Sentence and a BudgetValue.
 * A task references its parent and an optional causal factor (usually an Operation instance).  These are implemented as WeakReference to allow forgetting via the
 * garbage collection process.  Otherwise, Task ancestry would grow unbounded,
 * violating the assumption of insufficient resources (AIKR).
 * <p>
 * TODO decide if the Sentence fields need to be Reference<> also
 */
public interface Task<T extends Compound> extends Sentence<T>, Itemized<Sentence<T>>, Truthed {


    public static void getExplanation(Task task, int indent, StringBuilder sb) {
        //TODO StringBuilder

        for (int i = 0; i < indent; i++)
            sb.append("  ");


        task.appendTo(sb);

        List l = task.getLog();
        if (l!=null)
            sb.append(" log=").append(l);

        if (task.getCause() != null)
            sb.append(" cause=").append(task.getCause());

        if (task.getBestSolution() != null) {
            if (!task.getTerm().equals(task.getBestSolution().getTerm())) {
                sb.append(" solution=");
                task.getBestSolution().appendTo(sb);
            }
        }

        Task pt = task.getParentTask();

        Sentence pb = task.getParentBelief();
        if (pb != null) {
            if (pt != null && pb.equals(pt)) {

            } else {
                sb.append(" parentBelief=");
                task.getParentBelief().appendTo(sb);
            }
        }
        sb.append('\n');

        if (pt != null) {
            getExplanation(pt, indent + 1, sb);
        }
    }

    public static Set<Truthed> getSentences(Iterable<Task> tasks) {


        int size;

        if (tasks instanceof Collection)
            size = ((Collection)tasks).size();
        else
            size = 2;

        Set<Truthed> s = Global.newHashSet(size);
        for (Task t : tasks)
            s.add(t);
        return s;
    }

    public Task getParentTask();

    public Reference<Task> getParentTaskRef();

//    /**
//     * Constructor for an activated task
//     *
//     * @param s            The sentence
//     * @param b            The budget
//     * @param parentTask   The task from which this new task is derived
//     * @param parentBelief The belief from which this new task is derived
//     * @param solution     The belief to be used in future logic
//     */
//    public Task(final Sentence<T> s, final Budget b, final Task parentTask, final Sentence parentBelief, final Sentence solution) {
//        this(s, b, parentTask == null ? null : Global.reference(parentTask), parentBelief, solution);
//    }
//
//    public Task(T term, char punc, Truth truth, AbstractStamper stamp, final Budget b, final Task parentTask, final Sentence parentBelief, final Sentence solution) {
//        this(new Sentence(term, punc, truth, stamp), b, parentTask == null ? null : Global.reference(parentTask), parentBelief, solution);
//    }


//    @Override
//    public Task clone() {
//
//        if (sentence == null)
//            return this;
//
//        return new Task(sentence.clone(), this, parentTask, parentBelief, bestSolution);
//    }

//    public <X extends Compound> Task<X> clone(final Sentence<X> replacedSentence) {
//        return new Task(replacedSentence, this, parentTask, parentBelief, bestSolution);
//    }

    public Task getParentBelief();

    public Reference<Task> getParentBeliefRef();

    /** clones this Task with a new Term */
    default public <X extends Compound> Task<X> clone(final X t) {
        return clone(t, true);
    }

    default public <X extends Compound> Task<X> clone(X t, boolean cloneEvenIfTruthEqual) {
        return clone(t, getTruth(), cloneEvenIfTruthEqual);
    }

//    default public Task cloneEternal() {
//        return clone(getTerm(), TruthFunctions.eternalize(getTruth()), Stamp.ETERNAL);
//    }
//
//    default public <X extends Compound> Task<X> clone(X t, Truth newTruth) {
//        return clone(t, newTruth, true);
//    }
//
//    default public Task clone(long newOccurrenceTime) {
//        return clone(getTerm(), getTruth(), newOccurrenceTime);
//    }

    /** clones this Task with a new Term and truth  */
    default public Task clone(Compound newTerm, Truth newTruth, boolean cloneEvenIfTruthEqual) {
        return clone(newTerm, newTruth, getOccurrenceTime());
    }

    default public <X extends Compound> Task<X> clone(X t, Truth newTruth, long occ) {
        return clone(t, newTruth, occ, true);
    }



//    @Override
//    public boolean equals(final Object obj) {
//        if (obj == this) return true;
//        if (obj instanceof Sentence) {
//            Task t = (Task) obj;
//            return super.equals(t);// && equalParents(t);
//        }
//        return false;
//    }

    default public Task clone(Compound t, Truth newTruth, long occ, boolean cloneEvenIfTruthEqual) {
        if (newTruth instanceof ProjectedTruth) {
            long target = ((ProjectedTruth) newTruth).getTargetTime();
            if (occ!=target) {
                cloneEvenIfTruthEqual = true;
                occ = target;
            }
        }

        if (!cloneEvenIfTruthEqual) {
            if (occ == getOccurrenceTime() && getTruth().equals(newTruth) && getTerm().equals(t))
                return this;
        }

        Task tt = new DefaultTask<>(t, getPunctuation(), newTruth,
                getPriority(), getDurability(), getQuality(),
                getParentTaskRef(), getParentBeliefRef(), getBestSolutionRef()
        );
        tt.setTemporalInducting(isTemporalInductable());
        tt.setCause(getCause());

        //tt.setLastForgetTime(getLastForgetTime());

        tt.setEvidence(getEvidence());
        tt.setCyclic(isCyclic());

        tt.setCreationTime(getCreationTime());
        tt.setOccurrenceTime(occ);
        tt.log(getLog());
        return tt;
    }

    /** clones this Task with a new truth */
    default public Task<T> clone(Truth newTruth, boolean cloneEvenIfTruthEqual) {
        if (!cloneEvenIfTruthEqual) {
            if (getTruth().equals(newTruth)) return this;
        }
        return clone(getTerm(), newTruth, getOccurrenceTime());
    }

//    public boolean aboveThreshold() {
//        return budget.aboveThreshold();
//    }
/*    public boolean aboveThreshold(float additionalPriority) {
        return budget.aboveThreshold(additionalPriority);
    }*/

    default public boolean equalParents(final Task t) {
        Task p = getParentTask();
        Task tp = t.getParentTask();
        if (p == null) {
            return (tp == null);
        } else {
            return p.equals(tp);
        }
    }




    /**
     * Check if a Task is derived by a StructuralRule
     *
     * @return Whether the Task is derived by a StructuralRule
     */
//    public boolean isStructural() {
//        return (parentBelief == null) && (parentTask != null);
//    }



    public Sentence getBestSolution();

    public Reference<Task> getBestSolutionRef();

    default public StringBuilder toString(@Nullable Memory memory) {
        return appendTo(null, memory);
    }

    @Override
    default public StringBuilder appendTo(StringBuilder sb, @Nullable Memory memory) {
        if (sb == null) sb = new StringBuilder();
        return toString(sb, memory, false);
    }

    default public StringBuilder toString(StringBuilder buffer, @Nullable final Memory memory, final boolean term, final boolean showStamp, boolean showBudget) {


        String contentName;
        if (term && getTerm()!=null) {
            contentName = getTerm().toString();
        }
        else contentName = "";

        final CharSequence tenseString;
        if (memory!=null) {
            tenseString = getTense(memory.time(), memory.duration());
        }
        else {
            appendOccurrenceTime((StringBuilder) (tenseString = new StringBuilder()));
        }


        CharSequence stampString = showStamp ? stampAsStringBuilder() : null;

        int stringLength = contentName.length() + tenseString.length() + 1 + 1;

        if (getTruth() != null)
            stringLength += 11;

        if (showStamp)
            stringLength += stampString.length()+1;

        /*if (showBudget)*/ {
            //"$0.8069;0.0117;0.6643$ "
            stringLength += 1 + 6 + 1 + 6 + 1 + 6 + 1  + 1;
        }

        if (buffer == null)
            buffer = new StringBuilder(stringLength);
        else
            buffer.ensureCapacity(stringLength);


        if (showBudget) {
            getBudget().toBudgetStringExternal(buffer).append(" ");
        }

        buffer.append(contentName).append(getPunctuation());

        if (tenseString.length() > 0)
            buffer.append(' ').append(tenseString);

        if (getTruth()!= null) {
            buffer.append(' ');
            getTruth().appendString(buffer, 2);
        }

        if (showStamp)
            buffer.append(' ').append(stampString);

        return buffer;
    }


    default public boolean hasParent(Task t) {
        if (getParentTask() == null)
            return false;
        Task p = getParentTask();
        do {
            Task n = p.getParentTask();
            if (n == null) break;
            if (n.equals(t))
                return true;
            p = n;
        } while (true);
        return false;
    }

    default public Task getRootTask() {
        if (getParentTask() == null) {
            return null;
        }
        Task p = getParentTask();
        do {
            Task n = p.getParentTask();
            if (n == null) break;
            p = n;
        } while (true);
        return p;
    }

    public Operation getCause();

    public Task setCause(final Operation op);

    default public String getExplanation() {
        StringBuilder sb = new StringBuilder();
        return getExplanation(sb).toString();
    }

    default public StringBuilder getExplanation(StringBuilder temporary) {
        temporary.setLength(0);
        getExplanation(this, 0, temporary);
        return temporary;
    }


//    /**
//     * Get a String representation of the Task
//     *
//     * @return The Task as a String
//     */
//    @Override
//    public String toStringLong() {
//        final StringBuilder s = new StringBuilder();
//        s.append(super.toString()).append(' ').append(sentence.stamp.name());
//
//        Task pt = getParentTask();
//        if (pt != null) {
//            s.append("  \n from task: ").append(pt.toStringExternal());
//            if (parentBelief != null) {
//                s.append("  \n from belief: ").append(parentBelief.toString());
//            }
//        }
//        if (bestSolution != null) {
//            s.append("  \n solution: ").append(bestSolution.toString());
//        }
//        return s.toString();
//    }


//    /** returns the goal term for this task, which may be either the predicate of a forward implication,
//     * an operation.  if neither, returns null      */
//    public Term getGoalTerm() {
//        Term t = getContent();
//        if (t instanceof Implication) {
//            Implication i = (Implication)t;
//            if (i.getTemporalOrder() == TemporalRules.ORDER_FORWARD)
//                return i.getPredicate();
//            else if (i.getTemporalOrder() == TemporalRules.ORDER_BACKWARD) {
//                throw new RuntimeException("Term getGoal reversed");
//            }
//        }
//        else if (t instanceof Operation)
//            return t;
//        else if (Executive.isSequenceConjunction(t))
//            return t;
//        
//        return null;
//    }
//

    default public Truth getDesire() {
        return getTruth();
    }

    /**
     * signaling that the Task has ended or discarded
     */
    public void delete();

    public Task setTemporalInducting(boolean b);

    public boolean isTemporalInductable();

    default public void logUnrepeated(String reason) {
        if (getLog()!=null &&
                getLog().get(getLog().size()-1).equals(reason))
            return;
        log(reason);
    }

    public void log(String reason);
    public Task log(List<String> historyToCopy);
    public List<String> getLog();


    //TODO make a Source.{ INPUT, SINGLE, DOUBLE } enum

    /** is double-premise */
    public boolean isDouble();

    /** is single premise */
    boolean isSingle();

    /**
     * Check if a Task is a direct input
     *
     * @return Whether the Task is derived from another task
     */
    @Override default public boolean isInput() {
        return getParentTask() == null;
    }


    /**
     * a task is considered amnesiac (origin not rememebered) if its parent task has been forgotten (garbage collected via a soft/weakref)
     */
    default public boolean isAmnesiac() {
        return !isInput() && getParentTask() == null;
    }


    boolean isNormalized();

    /** updates all implied fields and re-hashes; returns this task */
    public Task normalized();


    @Deprecated default public boolean init(final Memory memory) {

        if (!isCommand()) {

            //if a task has an unperceived creationTime,
            // set it to the memory's current time here,
            // and adjust occurenceTime if it's not eternal

            if (getCreationTime() <= Stamp.TIMELESS) {
                final long now = memory.time();
                long oc = getOccurrenceTime();
                if (oc != Stamp.ETERNAL)
                    oc += now;

                setTime(now, oc);
            }

            if (getDuration() == 0)
                setDuration(memory.duration());

            if (getEvidence() == null) {
                setEvidence(memory.newStampSerial());
            }

            switch (getPunctuation()) {
                case Symbols.JUDGMENT:
                case Symbols.QUESTION:
                case Symbols.QUEST:
                case Symbols.GOAL:
                case Symbols.COMMAND:
                    break;
                default:
                    throw new RuntimeException("Invalid sentence punctuation");
            }

            if (isJudgmentOrGoal() && (getTruth() == null)) {
                throw new RuntimeException("Judgment and Goal sentences require non-null truth value");
            }

            if ((getParentTaskRef() != null && getParentTask() == null))
                throw new RuntimeException("parentTask must be null itself, or reference a non-null Task");

        /*
        if (t.equals( t.getParentTask()) ) {
            throw new RuntimeException(t + " has parentTask equal to itself");
        }
        */

            if (getEvidence()==null)
                throw new RuntimeException(this + " from premise " + getParentTask() + "," + getParentBelief()
                        + " yet no evidence provided");

            if (Global.DEBUG) {
                if (Sentence.invalidSentenceTerm(getTerm())) {
                    throw new RuntimeException("Invalid sentence content term: " + getTerm());
                }
            }

        }





        return normalized() != null;
    }




    default public Task projectTask(final long targetTime, final long currentTime) {

        final ProjectedTruth t = projection(targetTime, currentTime);

        return clone(getTerm(), t, t.getTargetTime());
    }





    @Override
    default public int getTemporalOrder() {
        return getTerm().getTemporalOrder();
    }

    public void setTruth(Truth t);
    public void discountConfidence();


    void setBestSolution(Memory memory, Task belief);


    public boolean isDeleted();
}
