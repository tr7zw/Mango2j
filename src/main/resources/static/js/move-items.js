$(() => {
    const printTitlesRecursively = (titles, selector) => {
        const select = $(selector);
        titles.forEach((title) => {
            const option = $('<option>').text(title.title);
            option.attr('value', title.dir);
            select.append(option);
            if (title.titles.length > 0) {
                printTitlesRecursively(title.titles, selector);
            }
        });
    };

    $.ajax({
        type: 'GET',
        url: `${location.protocol}//${location.host}${base_url}api/titles`,
    })
        .done((data) => {
            if (data.error) {
                alert(
                    'danger',
                    `Failed to move title. Error: ${data.error}`,
                );
                return;
            }
            const select = $('#move-select');
            const option = $('<option>').text("Move Entry to...");
            select.append(option);
            printTitlesRecursively(data.titles, '#move-select');
        });
});