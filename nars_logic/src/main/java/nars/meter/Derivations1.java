package nars.meter;

import nars.model.impl.Default;
import nars.NAR;
import nars.Global;
import nars.io.in.LibraryInput;
import nars.testing.TestNAR;

public class Derivations1  {



    public static void main(String[] args) {

        Global.DEBUG = true;

        Derivations d = new Derivations(false, false);


        for (int seed = 0; seed < 4; seed++) {
            for (String s : LibraryInput.getPaths("test2")) {
                NAR n = new TestNAR(new Default().setInternalExperience(null).level(3));
                n.memory.randomSeed(seed);
                d.record(n);
                n.input(LibraryInput.getExample(s));
                n.run(200);
            }
        }

        d.print(System.out);
    }
}
