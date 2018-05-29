// ==UserScript==
// @name         MouseTracker
// @namespace    http://tampermonkey.net/
// @version      0.1
// @description  Capture mouse movements and keypresses, count click and track page requests. Reset counter to 0 with F2. Dump all to the console.
// @author       Roman Gruber, Yannick Seitz, Jonas Wyss
// @match        http*://*/*
// @grant GM_setValue
// @grant GM_getValue
// @require http://code.jquery.com/jquery-1.12.4.min.js
// ==/UserScript==

var pushState = history.pushState;

// initialize counter value or get the current value in the cookie
var counter = GM_getValue("counter", 0);

// get the page change when the browser history changed
history.pushState = function () {
    pushState.apply(history, arguments);
    setTimeout(pageLoad("st"), 1000); // hacky delay tactics FTW!!
};

/*
 * register all that shit baby
 */
(function() {
    window.addEventListener("mousemove",mouseMove, true);
    window.addEventListener("click",mouseClick, true);
    document.addEventListener("keydown",flushData, true);
    window.addEventListener("popstate", pageLoad("b"), false)

    // jQuerys document ready state when the page changes
    $( document ).ready( pageLoad("jq") );
})();

/*
 * returns the current timestamp in nanoseconds if available
 */
function timestamp() {
    var timeStampInMs = window.performance && window.performance.now && window.performance.timing && window.performance.timing.navigationStart ? window.performance.now() + window.performance.timing.navigationStart : Date.now();
    return timeStampInMs;
}

/*
 * log the position of the mouse
 */
function mouseMove(e){
    console.info("move-->", timestamp(), e.pageX, e.pageY)
}

/*
 * reset the click counter to 0 when F2 is pressed and
 * log the keystrokes
 */
function flushData(e){
    console.info("key--->", timestamp(), e.keyCode);
    if(e.keyCode == 113){
        counter = 0;
        GM_setValue("counter", counter);
        console.info("+--------------------+")
        console.info("| Counter reset to 0 |")
        console.info("+--------------------+")
    }
}

/*
 * log the clicks and update the cookie with the current number of clicks
 */
function mouseClick(e){
    console.info("click->", timestamp(), counter++)
    GM_setValue("counter", counter);
}

/*
 * log the pageloads
 */
function pageLoad(t){
    console.info("load--> ", timestamp(), document.location.href);
}
