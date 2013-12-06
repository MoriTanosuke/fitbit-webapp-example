<html>
<head>
    <script type="text/javascript" src="//www.google.com/jsapi"></script>
    <script type="text/javascript">
        google.load('visualization', '1.0', {'packages':['corechart']});
        google.setOnLoadCallback(drawChart);

        function drawChart() {
            var data = new google.visualization.DataTable();
            data.addColumn('date', 'Date');
            data.addColumn('number', 'Steps');
            data.addRows([
<#list data as d>
                [new Date('${d.dateTime}'), ${d.value}],
</#list>
            ]);

            var options = {'title':'Steps', 'width':document.width - 50, 'height':document.height - 150};
            var chart = new google.visualization.ColumnChart(document.getElementById('chart_div'));
            chart.draw(data, options);
        }
    </script>
</head>
<body>
    <div id="chart_div"></div>
</body>
<html>
