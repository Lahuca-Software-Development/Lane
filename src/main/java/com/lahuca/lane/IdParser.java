package com.lahuca.lane;

/**
 * Converters for mapping longs to easy strings.
 * This is only in one direction, as due to truncation/reduction, precision is decreased.
 */
public class IdParser {

    /**
     * Maps the given ID to an easy human understandable character encoding.
     * @param id the id.
     * @param reduce the reduction from the left side of the id.
     * @param radix the radix of the output. Cannot be bigger than 36.
     * @param length the minimum length of the output.
     * @param lengthFill the character that is appended at the end to retrieve the length.
     * @return the character encoding.
     */
    public static String parseId(long id, int reduce, int radix, int length, char lengthFill) {
        String mapped = Long.toUnsignedString(id);
        if(reduce > 0 && reduce < mapped.length()) {
            mapped = mapped.substring(reduce);
        }
        mapped = new StringBuilder(mapped).reverse().toString();
        // We have reduced the original id by X characters, and then reversed the digits.
        mapped = Long.toUnsignedString(Long.parseLong(mapped), radix);
        // 'z' is used to map the length to the final length.
        StringBuilder extended = new StringBuilder(mapped.toUpperCase());
        while (extended.length() < length) {
            extended.append(lengthFill);
        }
        return extended.toString();
    }

    /**
     * Maps the given ID to an easy human understandable character encoding.
     * It uses radix 36, where 'Z' is used as fill character.
     * When a {@link System#currentTimeMillis()} (millisecond precision) value is given these are the lengths according to the reduction and the precision of the IDs:
     * <ul>
     *     <li>Reduction 0 (total length: 13): Output length 9: Precision 31 years</li>
     *     <li>Reduction 1 (total length: 12): Output length 8: Precision 3 years</li>
     *     <li>Reduction 2 (total length: 11): Output length 8: Precision 16 weeks</li>
     *     <li>Reduction 3 (total length: 10): Output length 7: Precision 11 days</li>
     *     <li>Reduction 4 (total length: 9): Output length 6: Precision 27 hours</li>
     *     <li>Reduction 5 (total length: 8): Output length 6: Precision 166 minutes</li>
     *     <li>Reduction 6 (total length: 7): Output length 5: Precision 16 minutes</li>
     *     <li>Reduction 7 (total length: 6): Output length 4: Precision 100 seconds</li>
     *     <li>Reduction 8 (total length: 5): Output length 4: Precision 10 seconds</li>
     *     <li>Reduction 9 (total length: 4): Output length 3: Precision 1 second</li>
     *     <li>Reduction 10 (total length: 3): Output length 2: Precision 100 milliseconds</li>
     *     <li>Reduction 11 (total length: 2): Output length 2: Precision 10 milliseconds</li>
     * </ul>
     * @param id the id.
     * @param reduce the reduction from the left side of the id.
     * @param length the minimum length of the output.
     * @return the character encoding.
     */
    public static String parseId(long id, int reduce, int length) {
        return parseId(id, reduce, 35, length, 'Z');
    }

    /**
     * Maps the given ID to an easy human understandable character encoding.
     * It reduces by 4 characters, uses radix 36, where 'Z' is used as fill character, the length has a minimum of 6 characters.
     * When a {@link System#currentTimeMillis()} (millisecond precision) value is given, this timestamp is truncated (reduced) to a timestamp of a long length 9.
     * This output is always of 6 characters in that case.
     * This timestamp has a precision (it repeats every) 27 hours, resulting in unique parsed Strings at least for a whole day every millisecond.
     * The maximum value that is being parsed is "J1DLIJ".
     * The timestamp of when this is written (1742067797694) is being parsed to "9G246P".
     * @param id the id.
     * @return the character encoding.
     */
    public static String parseId(long id) {
        return parseId(id, 4, 35, 6, 'Z');
    }

}
