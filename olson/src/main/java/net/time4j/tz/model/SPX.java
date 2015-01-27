/*
 * -----------------------------------------------------------------------
 * Copyright © 2013-2015 Meno Hochschild, <http://www.menodata.de/>
 * -----------------------------------------------------------------------
 * This file (SPX.java) is part of project Time4J.
 *
 * Time4J is free software: You can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * Time4J is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Time4J. If not, see <http://www.gnu.org/licenses/>.
 * -----------------------------------------------------------------------
 */

package net.time4j.tz.model;

import net.time4j.Month;
import net.time4j.PlainTime;
import net.time4j.PlainTimestamp;
import net.time4j.SystemClock;
import net.time4j.Weekday;
import net.time4j.base.MathUtils;
import net.time4j.tz.ZonalOffset;
import net.time4j.tz.ZonalTransition;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.time4j.PlainTime.SECOND_OF_DAY;


/**
 * <p><i>Serialization Proxy</i> f&uuml;r die Zeitzonenhistorie. </p>
 *
 * @author  Meno Hochschild
 * @since   2.2
 * @serial  include
 */
final class SPX
    implements Externalizable {

    //~ Statische Felder/Initialisierungen --------------------------------

    /** Serialisierungstyp von {@code FixedDayPattern}. */
    static final int FIXED_DAY_PATTERN_TYPE = 120;

    /** Serialisierungstyp von {@code DayOfWeekInMonthPattern}. */
    static final int DAY_OF_WEEK_IN_MONTH_PATTERN_TYPE = 121;

    /** Serialisierungstyp von {@code LastDayOfWeekPattern}. */
    static final int LAST_DAY_OF_WEEK_PATTERN_TYPE = 122;

    /** Serialisierungstyp von {@code RuleBasedTransitionModel}. */
    static final int RULE_BASED_TRANSITION_MODEL_TYPE = 125;

    /** Serialisierungstyp von {@code ArrayTransitionModel}. */
    static final int ARRAY_TRANSITION_MODEL_TYPE = 126;

    /** Serialisierungstyp von {@code CompositeTransitionModel}. */
    static final int COMPOSITE_TRANSITION_MODEL_TYPE = 127;

    private static final long POSIX_TIME_1825 =
        PlainTimestamp.of(1825, 1, 1, 0, 0).atUTC().getPosixTime();
    private static final long DAYS_IN_18_BITS = 86400L * 365 * 718;
    private static final long QUARTERS_IN_24_BITS = 15040511099L;
    private static final int NO_COMPRESSION = 0;

    private static final long serialVersionUID = 6526945678752534989L;

    //~ Instanzvariablen --------------------------------------------------

    private transient Object obj;
    private transient int type;

    //~ Konstruktoren -----------------------------------------------------

    /**
     * <p>Benutzt in der Deserialisierung gem&auml;&szlig; dem Kontrakt
     * von {@code Externalizable}. </p>
     */
    public SPX() {
        super();

    }

    /**
     * <p>Benutzt in der Serialisierung (writeReplace). </p>
     *
     * @param   obj     object to be serialized
     * @param   type    serialization type corresponding to type of obj
     */
    SPX(
        Object obj,
        int type
    ) {
        super();

        this.obj = obj;
        this.type = type;

    }

    //~ Methoden ----------------------------------------------------------

    /**
     * <p>Implementation method of interface {@link Externalizable}. </p>
     *
     * <p>The first byte contains the type of the object to be serialized.
     * Then the data bytes follow in a bit-compressed representation. </p>
     *
     * @serialData  data layout see {@code writeReplace()}-method of object
     *              to be serialized
     * @param       out     output stream
     * @throws      IOException
     */
    /*[deutsch]
     * <p>Implementierungsmethode des Interface {@link Externalizable}. </p>
     *
     * <p>Das erste Byte enth&auml;lt den Typ des zu serialisierenden Objekts.
     * Danach folgen die Daten-Bits in einer bit-komprimierten Darstellung. </p>
     *
     * @serialData  data layout see {@code writeReplace()}-method of object
     *              to be serialized
     * @param       out     output stream
     * @throws      IOException
     */
    @Override
    public void writeExternal(ObjectOutput out)
        throws IOException {

        out.writeByte(this.type);

        switch (this.type) {
            case FIXED_DAY_PATTERN_TYPE:
                writeFixedDayPattern(this.obj, out);
                break;
            case DAY_OF_WEEK_IN_MONTH_PATTERN_TYPE:
                writeDayOfWeekInMonthPattern(this.obj, out);
                break;
            case LAST_DAY_OF_WEEK_PATTERN_TYPE:
                writeLastDayOfWeekPattern(this.obj, out);
                break;
            case RULE_BASED_TRANSITION_MODEL_TYPE:
                writeRuleBasedTransitionModel(this.obj, out);
                break;
            case ARRAY_TRANSITION_MODEL_TYPE:
                writeArrayTransitionModel(this.obj, out);
                break;
            case COMPOSITE_TRANSITION_MODEL_TYPE:
                writeCompositeTransitionModel(this.obj, out);
                break;
            default:
                throw new InvalidClassException("Unknown serialized type.");
        }

    }

    /**
     * <p>Implementation method of interface {@link Externalizable}. </p>
     *
     * @param   in      input stream
     * @throws  IOException
     * @throws  ClassNotFoundException
     */
    /*[deutsch]
     * <p>Implementierungsmethode des Interface {@link Externalizable}. </p>
     *
     * @param   in      input stream
     * @throws  IOException
     * @throws  ClassNotFoundException
     */
    @Override
    public void readExternal(ObjectInput in)
        throws IOException, ClassNotFoundException {

        int header = in.readByte();

        switch (header) {
            case FIXED_DAY_PATTERN_TYPE:
                this.obj = readFixedDayPattern(in);
                break;
            case DAY_OF_WEEK_IN_MONTH_PATTERN_TYPE:
                this.obj = readDayOfWeekInMonthPattern(in);
                break;
            case LAST_DAY_OF_WEEK_PATTERN_TYPE:
                this.obj = readLastDayOfWeekPattern(in);
                break;
            case RULE_BASED_TRANSITION_MODEL_TYPE:
                this.obj = readRuleBasedTransitionModel(in);
                break;
            case ARRAY_TRANSITION_MODEL_TYPE:
                this.obj = readArrayTransitionModel(in);
                break;
            case COMPOSITE_TRANSITION_MODEL_TYPE:
                this.obj = readCompositeTransitionModel(in);
                break;
            default:
                throw new StreamCorruptedException("Unknown serialized type.");
        }

    }

    // called by CompositeTransitionModel and ArrayTransitionModel
    static void writeTransitions(
        ZonalTransition[] transitions,
        int size,
        DataOutput out
    ) throws IOException {

        int n = Math.min(size, transitions.length);
        out.writeInt(n);

        if (n > 0) {
            int stdOffset = transitions[0].getPreviousOffset();
            writeOffset(out, stdOffset);

            for (int i = 0; i < n; i++) {
                stdOffset = writeTransition(transitions[i], stdOffset, out);
            }
        }

    }

    // called by TZRepositoryCompiler
    static void writeTransitions(
        List<ZonalTransition> transitions,
        DataOutput out
    ) throws IOException {

        int n = transitions.size();
        out.writeInt(n);

        if (n > 0) {
            int stdOffset = transitions.get(0).getPreviousOffset();
            writeOffset(out, stdOffset);

            for (int i = 0; i < n; i++) {
                stdOffset = writeTransition(transitions.get(i), stdOffset, out);
            }
        }

    }

    // called by
    // TZRepositoryProvider, CompositeTransitionModel and ArrayTransitionModel
    static List<ZonalTransition> readTransitions(ObjectInput in)
        throws IOException {

        int n = in.readInt();

        if (n == 0) {
            return Collections.emptyList();
        }

        List<ZonalTransition> transitions = new ArrayList<ZonalTransition>(n);
        int previous = readOffset(in);
        int rawOffset = previous;
        long oldTsp = Long.MIN_VALUE;

        for (int i = 0; i < n; i++) {
            int first = in.readByte();
            boolean newStdOffset = (first < 0);
            int dstIndex = ((first >>> 5) & 3);
            int timeIndex = ((first >>> 2) & 7);
            int tod;

            switch (timeIndex) {
                case 1:
                    tod = 0;
                    break;
                case 2:
                    tod = 60;
                    break;
                case 3:
                    tod = 3600;
                    break;
                case 4:
                    tod = 7200;
                    break;
                case 5:
                    tod = 10800;
                    break;
                case 6:
                    tod = 14400;
                    break;
                case 7:
                    tod = 18000;
                    break;
                default:
                    tod = -1;
            }

            long posix;

            if (tod == -1) {
                posix = in.readLong();
            } else {
                int dayIndex = ((first & 3) << 16);
                dayIndex |= ((in.readByte() & 0xFF) << 8);
                dayIndex |= (in.readByte() & 0xFF);
                posix = ((dayIndex * 86400L) + POSIX_TIME_1825 + tod - 3600);
                posix -= rawOffset;
            }

            if (posix <= oldTsp) {
                throw new StreamCorruptedException(
                    "Wrong order of transitions.");
            } else {
                oldTsp = posix;
            }

            int dstOffset;

            switch (dstIndex) {
                case 1:
                    dstOffset = 0;
                    break;
                case 2:
                    dstOffset = 3600;
                    break;
                case 3:
                    dstOffset = 7200;
                    break;
                default:
                    dstOffset = readOffset(in);
            }

            if (newStdOffset) {
                rawOffset = readOffset(in);
            }

            int total = rawOffset + dstOffset;
            ZonalTransition transition =
                new ZonalTransition(posix, previous, total, dstOffset);
            previous = total;
            transitions.add(transition);

        }

        return transitions;

    }

    // called by TZRepositoryCompiler,
    // CompositeTransitionModel and RuleBasedTransitionModel
    static void writeRules(
        List<DaylightSavingRule> rules,
        ObjectOutput out
    ) throws IOException {

        out.writeByte(rules.size());

        for (DaylightSavingRule rule : rules) {
            out.writeByte(rule.getType());

            switch (rule.getType()) {
                case FIXED_DAY_PATTERN_TYPE:
                    writeFixedDayPattern(rule, out);
                    break;
                case DAY_OF_WEEK_IN_MONTH_PATTERN_TYPE:
                    writeDayOfWeekInMonthPattern(rule, out);
                    break;
                case LAST_DAY_OF_WEEK_PATTERN_TYPE:
                    writeLastDayOfWeekPattern(rule, out);
                    break;
                default:
                    out.writeObject(rule);
            }
        }

    }

    // called by TZRepositoryProvider,
    // CompositeTransitionModel and RuleBasedTransitionModel
    static List<DaylightSavingRule> readRules(ObjectInput in)
        throws IOException, ClassNotFoundException {

        int n = in.readByte();

        if (n == 0) {
            return Collections.emptyList();
        }

        List<DaylightSavingRule> rules = new ArrayList<DaylightSavingRule>(n);
        DaylightSavingRule previous = null;

        for (int i = 0; i < n; i++) {
            int type = in.readByte();
            DaylightSavingRule rule;

            switch (type) {
                case FIXED_DAY_PATTERN_TYPE:
                    rule = readFixedDayPattern(in);
                    break;
                case DAY_OF_WEEK_IN_MONTH_PATTERN_TYPE:
                    rule = readDayOfWeekInMonthPattern(in);
                    break;
                case LAST_DAY_OF_WEEK_PATTERN_TYPE:
                    rule = readLastDayOfWeekPattern(in);
                    break;
                default:
                    rule = (DaylightSavingRule) in.readObject();
            }

            if (
                (previous != null)
                && (RuleComparator.INSTANCE.compare(previous, rule) >= 0)
            ) {
                throw new InvalidObjectException(
                    "Order of daylight saving rules is not ascending.");
            }

            previous = rule;
            rules.add(rule);
        }

        return rules;

    }

    private static void writeDaylightSavingRule(
        DataOutput out,
        DaylightSavingRule rule
    ) throws IOException {

        int tod = (rule.getTimeOfDay().get(SECOND_OF_DAY).intValue() << 8);
        int indicator = rule.getIndicator().ordinal();
        int dst = rule.getSavings();

        if (dst == 0) {
            out.writeInt(indicator | tod | 8);
        } else if (dst == 3600) {
            out.writeInt(indicator | tod | 16);
        } else {
            out.writeInt(indicator | tod);
            writeOffset(out, dst);
        }

    }

    private static void writeOffset(
        DataOutput out,
        int offset
    ) throws IOException {

        if ((offset % 900) == 0) {
            out.writeByte(offset / 900);
        } else {
            out.writeByte(127);
            out.writeInt(offset);
        }

    }

    private static int readOffset(DataInput in) throws IOException {

        int savings = in.readByte();

        if (savings == 127) {
            return in.readInt();
        } else {
            return savings * 900;
        }

    }

    private static int readSavings(
        byte offsetInfo,
        DataInput in
    ) throws IOException {

        if ((offsetInfo & 8) == 8) {
            return 0;
        } else if ((offsetInfo & 16) == 16) {
            return 3600;
        } else {
            return readOffset(in);
        }

    }

    private static void writeFixedDayPattern(
        Object rule,
        DataOutput out
    ) throws IOException {

        FixedDayPattern pattern = (FixedDayPattern) rule;
        out.writeByte(pattern.getMonth());
        out.writeByte(pattern.getDayOfMonth());
        writeDaylightSavingRule(out, pattern);

    }

    private static DaylightSavingRule readFixedDayPattern(DataInput in)
        throws IOException, ClassNotFoundException {

        int month = in.readByte();
        int dayOfMonth = in.readByte();

        int timeInfo = in.readInt();
        PlainTime timeOfDay =
            PlainTime.midnightAtStartOfDay().with(SECOND_OF_DAY, timeInfo >> 8);
        byte offsetInfo = (byte) (timeInfo & 0xFF);
        OffsetIndicator indicator = OffsetIndicator.VALUES[offsetInfo & 7];
        int savings = readSavings(offsetInfo, in);

        return new FixedDayPattern(
            Month.valueOf(month),
            dayOfMonth,
            timeOfDay,
            indicator,
            savings);

    }

    private static void writeDayOfWeekInMonthPattern(
        Object rule,
        DataOutput out
    ) throws IOException {

        DayOfWeekInMonthPattern pattern = (DayOfWeekInMonthPattern) rule;
        out.writeByte(pattern.getMonth());
        out.writeByte(pattern.getDayOfMonth());

        int dow = pattern.getDayOfWeek();

        if (pattern.isAfter()) {
            dow = -dow;
        }

        out.writeByte(dow);
        writeDaylightSavingRule(out, pattern);

    }

    private static DaylightSavingRule readDayOfWeekInMonthPattern(DataInput in)
        throws IOException, ClassNotFoundException {

        Month month = Month.valueOf(in.readByte());
        int dayOfMonth = in.readByte();
        int dow = in.readByte();
        Weekday dayOfWeek = Weekday.valueOf(Math.abs(dow));
        boolean after = (dow < 0);

        int timeInfo = in.readInt();
        PlainTime timeOfDay =
            PlainTime.midnightAtStartOfDay().with(SECOND_OF_DAY, timeInfo >> 8);
        byte offsetInfo = (byte) (timeInfo & 0xFF);
        OffsetIndicator indicator = OffsetIndicator.VALUES[offsetInfo & 7];
        int savings = readSavings(offsetInfo, in);

        return new DayOfWeekInMonthPattern(
            month,
            dayOfMonth,
            dayOfWeek,
            timeOfDay,
            indicator,
            savings,
            after);

    }

    private static void writeLastDayOfWeekPattern(
        Object rule,
        DataOutput out
    ) throws IOException {

        LastDayOfWeekPattern pattern = (LastDayOfWeekPattern) rule;
        int data = (pattern.getDayOfWeek() << 4);
        data |= pattern.getMonth();
        out.writeByte(data);
        writeDaylightSavingRule(out, pattern);

    }

    private static DaylightSavingRule readLastDayOfWeekPattern(DataInput in)
        throws IOException, ClassNotFoundException {

        int data = in.readByte();
        Month month = Month.valueOf(data & 15);
        Weekday dayOfWeek = Weekday.valueOf(data >> 4);

        int timeInfo = in.readInt();
        PlainTime timeOfDay =
            PlainTime.midnightAtStartOfDay().with(SECOND_OF_DAY, timeInfo >> 8);
        byte offsetInfo = (byte) (timeInfo & 0xFF);
        OffsetIndicator indicator = OffsetIndicator.VALUES[offsetInfo & 7];
        int savings = readSavings(offsetInfo, in);

        return new LastDayOfWeekPattern(
            month,
            dayOfWeek,
            timeOfDay,
            indicator,
            savings);

    }

    private static void writeRuleBasedTransitionModel(
        Object obj,
        ObjectOutput out
    ) throws IOException {

        RuleBasedTransitionModel model = (RuleBasedTransitionModel) obj;
        ZonalTransition initial = model.getInitialTransition();
        long posixTime = initial.getPosixTime();

        if (
            (posixTime >= POSIX_TIME_1825)
            && (posixTime < POSIX_TIME_1825 + QUARTERS_IN_24_BITS)
            && ((posixTime % 900) == 0)
        ) {
            int data = (int) ((posixTime - POSIX_TIME_1825) / 900);
            out.writeByte((data >>> 16) & 0xFF);
            out.writeByte((data >>> 8) & 0xFF);
            out.writeByte(data & 0xFF);
        } else {
            out.writeByte(0xFF);
            out.writeLong(initial.getPosixTime());
        }

        writeOffset(out, initial.getPreviousOffset());
        writeOffset(out, initial.getTotalOffset());
        writeOffset(out, initial.getDaylightSavingOffset());
        writeRules(model.getRules(), out);

    }

    private static Object readRuleBasedTransitionModel(ObjectInput in)
        throws IOException, ClassNotFoundException {

        long posixTime;
        int high = in.readByte() & 0xFF;

        if (high == 0xFF) {
            posixTime = in.readLong();
        } else {
            int mid = in.readByte() & 0xFF;
            int low = in.readByte() & 0xFF;
            posixTime = ((high << 16) + (mid << 8) + low) * 900L;
            posixTime += POSIX_TIME_1825;
        }

        int previous = readOffset(in);
        int total = readOffset(in);
        int dst = readOffset(in);
        ZonalTransition initial =
            new ZonalTransition(posixTime, previous, total, dst);
        List<DaylightSavingRule> rules = readRules(in);

        return new RuleBasedTransitionModel(
            initial,
            rules,
            SystemClock.INSTANCE,
            false);

    }

    private static void writeArrayTransitionModel(
        Object obj,
        ObjectOutput out
    ) throws IOException {

        ArrayTransitionModel model = (ArrayTransitionModel) obj;
        model.writeTransitions(out);

    }

    private static Object readArrayTransitionModel(ObjectInput in)
        throws IOException, ClassNotFoundException {

        return new ArrayTransitionModel(
            readTransitions(in),
            SystemClock.INSTANCE,
            false,
            false);

    }

    private static void writeCompositeTransitionModel(
        Object obj,
        ObjectOutput out
    ) throws IOException {

        CompositeTransitionModel model = (CompositeTransitionModel) obj;
        model.writeTransitions(out);
        writeRules(model.getRules(), out);

    }

    private static Object readCompositeTransitionModel(ObjectInput in)
        throws IOException, ClassNotFoundException {

        List<ZonalTransition> transitions = readTransitions(in);

        return TransitionModel.of(
            ZonalOffset.ofTotalSeconds(transitions.get(0).getPreviousOffset()),
            transitions,
            readRules(in),
            SystemClock.INSTANCE,
            false,
            false);

    }

    private static int writeTransition(
        ZonalTransition transition,
        int stdOffset,
        DataOutput out
    ) throws IOException {

        int rawOffset = transition.getStandardOffset();
        boolean newStdOffset = (rawOffset != stdOffset);
        byte first = 0;

        if (newStdOffset) {
            first |= (1 << 7);
        }

        int dstIndex;

        switch (transition.getDaylightSavingOffset()) {
            case 0:
                dstIndex = 1;
                break;
            case 3600:
                dstIndex = 2;
                break;
            case 7200:
                dstIndex = 3;
                break;
            default:
                dstIndex = NO_COMPRESSION;
        }

        first |= (dstIndex << 5);

        // local standard time plus two hours: 22:00-3:00 => 0:00-5:00
        long modTime = transition.getPosixTime() + stdOffset + 3600;
        int timeIndex;

        if (
            (modTime >= POSIX_TIME_1825)
            && (modTime < POSIX_TIME_1825 + DAYS_IN_18_BITS) // 2542-07-11
        ) {
            int tod = MathUtils.floorModulo(modTime, 86400);

            switch (tod) {
                case 0:
                    timeIndex = 1;
                    break;
                case 60:
                    timeIndex = 2;
                    break;
                case 3600:
                    timeIndex = 3;
                    break;
                case 7200:
                    timeIndex = 4;
                    break;
                case 10800:
                    timeIndex = 5;
                    break;
                case 14400:
                    timeIndex = 6;
                    break;
                case 18000:
                    timeIndex = 7;
                    break;
                default:
                    timeIndex = NO_COMPRESSION;
            }
        } else {
            timeIndex = NO_COMPRESSION;
        }

        first |= (timeIndex << 2);

        if (timeIndex == NO_COMPRESSION) {
            out.writeByte(first);
            out.writeLong(transition.getPosixTime());
        } else {
            int dayIndex = (int) ((modTime - POSIX_TIME_1825) / 86400);
            byte high = (byte) ((dayIndex >>> 16) & 3);
            first |= high;
            out.writeByte(first);
            out.writeByte((dayIndex >>> 8) & 0xFF);
            out.writeByte(dayIndex & 0xFF);
        }

        if (dstIndex == NO_COMPRESSION) {
            writeOffset(out, transition.getDaylightSavingOffset());
        }

        if (newStdOffset) {
            writeOffset(out, rawOffset);
        }

        return rawOffset;

    }

    private Object readResolve() throws ObjectStreamException {

        return this.obj;

    }

}
