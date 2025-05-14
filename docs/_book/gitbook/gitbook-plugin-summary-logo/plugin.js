require(['gitbook', 'jQuery'], function (gitbook, $) {
    var url = ''
    var link = ''
    var target = ''
    var style = ''
    var insertLogo = function (url, link, target, style) {
        var _html = '<div class="book-logo">\
                  <a class="book-link" href="' + link + '" target="' + target + '">\
                    <img src="' + url + '" style="' + style + '" alt="" />\
                  </a>\
                </div>'
        $('.book-summary').children().eq(0).before(_html)
    }

    gitbook.events.bind('start', function (e, config) {
        url = gitbook.state.bookRoot + "/" + config['summary-logo']['logo']
        link = config['summary-logo']['link']
        target = config['summary-logo']['target']
        style = config['summary-logo']['style']
    })

    gitbook.events.bind("page.change", function () {
        insertLogo(url, link, target, style)
    })
})
