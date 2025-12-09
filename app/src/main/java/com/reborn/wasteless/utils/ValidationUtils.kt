package com.reborn.wasteless.utils

/**
 * Honestly- I had to search this up :skull: but this is
 * a pattern to check if it's a valid email
 * the [A-Za-z0-9+_.-]+ basically describes the list of allowed characters before @
 * "A-Za-z" is any uppercase/lowercase letter (A-Z & a-z)
 * "0-9" is digits
 * "+_.-" is just these 4 symbols being allowed as well
 * And the + outside means "one or more" of these characters
 *
 * @param email Email address to validate
 * @return true if email format is valid, false otherwise
 */
fun isValidEmail(email: String): Boolean {
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
    return email.matches(emailRegex.toRegex())
}

/**
 * //    Regexp                Description
 * ^                 # start-of-string
 * (?=.*[0-9])       # a digit must occur at least once
 * (?=.*[a-z])       # a lower case letter must occur at least once
 * (?=.*[A-Z])       # an upper case letter must occur at least once
 * (?=.*[@#$%^&+=])  # a special character must occur at least once you can replace with your special characters
 * (?=\\S+$)          # no whitespace allowed in the entire string
 * .{8,}             # anything, but length must be at least 8 characters though
 * $                 # end-of-string
 * ^ some stack overflow ans i saw lol
 *
 * @param password Password to validate
 * @return true if valid password format, else return false
 */
fun isValidPassword(password: String): Boolean {
    val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+=.<>,?/-])(?=\\S+$).{8,}$"
    return password.matches(passwordRegex.toRegex())
}