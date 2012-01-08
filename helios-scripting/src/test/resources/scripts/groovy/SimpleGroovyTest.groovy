/*
 * Test script for a groovy script instance.
 * Pass in an array of numbers, returns the sum of the array.
 * Whitehead, HeliosScripting, Jan 2011
 */
 
 // Expected binding name is "numbers".
 total = 0;
 numbers.each() {
 	total += it;
 }
 return total;