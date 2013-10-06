package org.jetbrains.jet.j2k.test;

import junit.framework.Test;
import junit.framework.TestSuite;

import static org.jetbrains.jet.j2k.test.TestPackage.suiteForDirectory;

public class ConverterTestSuite {

    private ConverterTestSuite() {
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(suiteForDirectory("tests/testData", "/ast", new NamedTestFactory() {
            public Test createTest(String dataPath, String name) {
                return new StandaloneJavaToKotlinConverterTest(dataPath, name);
            }
        }));
        return suite;
    }
}
