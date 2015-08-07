package nars.rover.run;

import automenta.vivisect.Video;
import nars.Global;
import nars.Memory;
import nars.NAR;
import nars.NARSeed;
import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import nars.bag.impl.experimental.ChainBag;
import nars.budget.Budget;
import nars.clock.SimulatedClock;
import nars.concept.Concept;
import nars.event.CycleReaction;
import nars.gui.NARSwing;
import nars.link.TaskLink;
import nars.link.TermLink;
import nars.link.TermLinkKey;
import nars.nar.Default;
import nars.nar.experimental.Equalized;
import nars.nar.experimental.Solid;
import nars.rover.Sim;
import nars.rover.robot.Rover;
import nars.rover.robot.Spider;
import nars.rover.robot.Turret;
import nars.task.Sentence;
import nars.term.Term;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import javax.swing.*;

/**
 * Created by me on 6/20/15.
 */
public class SomeRovers {

    private static SimulatedClock clock;

    static {
        Video.themeInvert();
    }

    public static NARSeed newSolid() {
        NARSeed d = new Solid(32, 599, 1, 1, 1, 3 ) {


        }.setInternalExperience(null).setClock(clock = new SimulatedClock());

        //TextOutput.out(nar).setShowInput(true).setShowOutput(false);

        //N/A for solid
        //nar.param.inputsMaxPerCycle.set(32);
        //nar.param.conceptsFiredPerCycle.set(4);


        d.conceptBeliefsMax.set(16);
        d.conceptGoalsMax.set(12);


        return d;
    }
    public static NARSeed newDefault() {
        Default d = new Equalized(8192, 96, 8) {


            @Override
            public Concept newConcept(final Term t, final Budget b, final Memory m) {

                Bag<Sentence, TaskLink> taskLinks =
                        new CurveBag(rng, /*sentenceNodes,*/ getConceptTaskLinks());
                        //new ChainBag(rng,  getConceptTaskLinks());

                Bag<TermLinkKey, TermLink> termLinks =
                        new CurveBag(rng, /*termlinkKeyNodes,*/ getConceptTermLinks());
                        //new ChainBag(rng, /*termlinkKeyNodes,*/ getConceptTermLinks());

                return newConcept(t, b, taskLinks, termLinks, m);
            }

        };
        //d.setInternalExperience(null);
        d.setClock(clock = new SimulatedClock());


        //TextOutput.out(nar).setShowInput(true).setShowOutput(false);

        d.setTermLinkBagSize(64);

        //N/A for solid
        //nar.param.inputsMaxPerCycle.set(32);
        //nar.param.conceptsFiredPerCycle.set(4);

        d.conceptCreationExpectation.set(0);
        d.conceptBeliefsMax.set(16);
        d.conceptGoalsMax.set(8);
        //d.termLinkForgetDurations.set(4);



        return d;
    }

    public static void main(String[] args) {

        Global.DEBUG = Global.EXIT_ON_EXCEPTION = false;


        float fps = 90;
        boolean cpanels = true;

        final Sim game = new Sim();


        game.add(new Turret("turret"));
        game.add(new Spider("spider",
                3, 3, 0.618f, 30, 30));


        int rovers = 1;

        for (int i = 0; i < rovers; i++)  {

            NAR nar;
            SimulatedClock clock;

            //NARSeed d = newSolid();
            NARSeed d = newDefault();
            nar = new NAR(d);

            //new InputActivationController(nar);

            int nc = 16;
            nar.setCyclesPerFrame(nc);
            nar.param.duration.set(nc/2);


            //nar.param.shortTermMemoryHistory.set(3);

            (nar.param).outputVolume.set(0);
            //nar.param.budgetThreshold.set(0.02);
            //nar.param.confidenceThreshold.set(0.02);

            //(nar.param).conceptForgetDurations.set(2f);
            /*
            (nar.param).taskLinkForgetDurations.set(10f);
            (nar.param).termLinkForgetDurations.set(10f);
            (nar.param).novelTaskForgetDurations.set(10f);
            */



            game.add(new Rover("r" + i, nar));

            if (cpanels) {
                SwingUtilities.invokeLater(() -> {
                    new NARSwing(nar, false);
                });
            }
        }

        game.run(fps);
    }

    private static class InputActivationController extends CycleReaction {

        private final NAR nar;

        final int windowSize;

        final DescriptiveStatistics busyness;

        public InputActivationController(NAR nar) {
            super(nar);
            this.nar = nar;
            this.windowSize = nar.memory.duration();
            this.busyness = new DescriptiveStatistics(windowSize);
        }

        @Override
        public void onCycle() {

            final float bInst = nar.memory.emotion.busy();
            busyness.addValue(bInst);

            float bAvg = (float)busyness.getMean();

            float busyMax = 3f;

            double a = nar.param.inputActivationFactor.get();
            if (bAvg > busyMax) {
                a -= 0.01f;
            }
            else  {
                a += 0.01f;
            }

            final float min = 0.01f;
            if (a < min) a = min;
            if (a > 1f) a = 1f;

            //System.out.println("act: " + a + " (" + bInst + "," + bAvg);

            nar.param.inputActivationFactor.set(a);
            nar.param.conceptActivationFactor.set( 0.5f * (1f + a) /** half as attenuated */ );
        }
    }
}
