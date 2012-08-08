/*******************************************************************************
 * Copyright (c) 2012 pf_miles.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     pf_miles - initial API and implementation
 ******************************************************************************/
package com.github.pfmiles.dropincc;

import junit.framework.TestCase;

import com.github.pfmiles.dropincc.calctest.Calculator;
import com.github.pfmiles.dropincc.impl.util.Pair;

/**
 * 
 * @author pf-miles
 * 
 */
public class LangTest extends TestCase {
    /**
     * A basic calculator test
     * 
     */
    public void testCalculator() {
        assertTrue(3389 == Calculator.compute("1   +2+3+(4   +5*6*7*(64/8/2/(2/1   )/1)*8   +9  )+   10"));
    }

    public void testIllegalToken() {
        try {
            Calculator.compute("1 +2+3^4");
            assertTrue(false);
        } catch (DropinccException e) {
            System.out.println("Error msg for test: " + e);
            assertTrue(true);
        }
    }

    public void testSyntaxErr() {
        try {
            Calculator.compute("1+2* \n (5+10.2 * + 7)*4");
            assertTrue(false);
        } catch (DropinccException e) {
            System.out.println("Error msg for test: " + e);
            assertTrue(true);
        }
    }

    /**
     * <pre>
     * S ::= A $
     * A ::= a
     *     |
     * </pre>
     */
    public void testEmptyAlternative() {
        // normal empty alt
        Lang lang = new Lang("Test");
        Grule A = lang.newGrule();
        lang.defineGrule(A, CC.EOF);
        A.define("a").alt(CC.NOTHING);
        Exe exe = lang.compile();
        assertTrue("a".equals(((Object[]) exe.eval("a"))[0]));

        // empty single alt
        lang = new Lang("Test");
        A = lang.newGrule();
        lang.defineGrule(A, CC.EOF);
        A.define(CC.NOTHING);
        exe = lang.compile();
        assertTrue(((Object[]) exe.eval(""))[0] == null);

        // with action
        // empty single alt
        lang = new Lang("Test");
        A = lang.newGrule();
        lang.defineGrule(A, CC.EOF);
        A.define(CC.NOTHING).action(new Action() {
            public Object act(Object matched) {
                assertTrue(matched == null);
                return matched;
            }
        });
        exe = lang.compile();
        assertTrue(((Object[]) exe.eval(""))[0] == null);

        // normal empty alt
        lang = new Lang("Test");
        A = lang.newGrule();
        lang.defineGrule(A, CC.EOF);
        A.define("a").action(new Action() {
            public Object act(Object matched) {
                assertTrue("a".equals(matched));
                return matched;
            }
        }).alt(CC.NOTHING).action(new ParamedAction<Object>() {
            public Object act(Object arg, Object matched) {
                assertTrue(matched == null);
                return matched;
            }
        });
        exe = lang.compile();
        assertTrue("a".equals(((Object[]) exe.eval("a"))[0]));
    }

    /**
     * Non LL-regular rule test; Test delayed actions and all actions should be
     * fired only once.
     * 
     * <pre>
     * S ::= A $
     * A ::= B c
     *     | B d
     * B ::= C B e
     *     | C B f
     *     | b
     * C ::= g C
     *     | g
     * </pre>
     */
    public void testNonLLRegularRule() {
        Lang lang = new Lang("Test");
        Grule A = lang.newGrule();
        lang.defineGrule(A, CC.EOF).action(new Action() {
            public Object act(Object matched) {
                return ((Object[]) matched)[0];
            }
        });
        Grule B = lang.newGrule();
        A.define(B, "c").action(new Action() {
            public Object act(Object matched) {
                Object[] ms = (Object[]) matched;
                return ((String) ms[0]) + ((String) ms[1]);
            }
        }).alt(B, "d").action(new Action() {
            public Object act(Object matched) {
                Object[] ms = (Object[]) matched;
                return ((String) ms[0]) + ((String) ms[1]);
            }
        });
        Grule C = lang.newGrule();
        B.define(C, B, "e").action(new Action() {
            public Object act(Object matched) {
                Object[] ms = (Object[]) matched;
                return ((String) ms[0]) + ((String) ms[1] + ((String) ms[2]));
            }

        }).alt(C, B, "f").action(new Action() {
            public Object act(Object matched) {
                Object[] ms = (Object[]) matched;
                return ((String) ms[0]) + ((String) ms[1] + ((String) ms[2]));
            }
        }).alt("b").action(new Action() {
            public Object act(Object matched) {
                return (String) matched;
            }
        });
        C.define("g", C).action(new ParamedAction<Pair<String, Integer>>() {
            public Object act(Pair<String, Integer> arg, Object matched) {
                arg.setRight(arg.getRight() + 1);
                Object[] ms = (Object[]) matched;
                return ((String) ms[0]) + ((String) ms[1]);
            }
        }).alt("g").action(new ParamedAction<Pair<String, Integer>>() {
            public Object act(Pair<String, Integer> arg, Object matched) {
                arg.setRight(arg.getRight() + 1);
                return matched;
            }
        });

        Exe exe = lang.compile();
        System.out.println(lang.getDebugMsgs());
        System.out.println(lang.getWarnings());
        assertTrue(lang.getWarnings() != null);
        Pair<String, Integer> gcount = new Pair<String, Integer>("g", 0);
        System.out.println(exe.eval("ggbfd", gcount));
        assertTrue(gcount.getRight() == 2);
        gcount.setRight(0);
        System.out.println(exe.eval("ggggggggggbfd", gcount));
        assertTrue(gcount.getRight() == 10);
    }
    
    
}
