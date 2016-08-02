package com.lucaszanella;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lucaszanella on 8/1/16.
 * Simple HTML table parser to parse tables as Strings, useful for Jsoup or other libraries that deal with
 * HTML nodes.
 */
public class JsoupTable {
    private List<Element> headers = new ArrayList<>();
    private List<List<Element>> rows = new ArrayList<>();

    public List<Element> getHeaders() {
        return this.headers;
    }

    public List<String> getHeaderNames() {
        List<String> headerNames = new ArrayList<>();
        for (Element header:this.headers) {
            headerNames.add(header.text());
        }
        return headerNames;
    }

    public List<Element> getRow(int index) {
        return this.rows.get(index);
    }

    public List<String> getRowStrings(int index) {
        List<String> rowStrings = new ArrayList<>();
        for (Element row:this.rows.get(index)) {
           rowStrings.add(row.text());
        }
        return rowStrings;
    }

    public List<List<Element>> getAllRows() {
        return this.rows;
    }

    public List<List<String>>  getAllRowStrings() {
        List<List<String>> allRowStrings = new ArrayList<>();
        int rowsSize = rows.size();
        for (int i = 0; i<rowsSize; i++) {
            allRowStrings.add(getRowStrings(i));
        }
        return allRowStrings;
    }

    private static void excludeFirstElement(Elements elements) {
        elements.first().remove();
    }

    private void parseRows(Elements rows) {

        for (Element row:rows) {
            List<Element> rowValuesList = new ArrayList<>();
            //getting td tag ensures that if this is a row with header names
            //nothing will be selected and so the for will neven happen
            Elements rowValues = row.getElementsByTag("td");
            for (Element rowValue:rowValues) {
                rowValuesList.add(rowValue);
            }
            //We'll only add this new row to rows if it's not empty
            if (!rowValues.isEmpty()) {
                this.rows.add(rowValuesList);
            }
        }
    }

    public JsoupTable(Element htmlTable) {
        Elements tableRows = htmlTable.getElementsByTag("tr");//All rows of the table
        Element tableHeader = tableRows.first();//First row, which normally contains a header
        //Elements tableColumns = htmlTable.select("td");//All columns
        if (!tableHeader.getElementsByTag("th").isEmpty()) {
            System.out.println("not empty header names");
            Elements tableHeaderValues = tableHeader.getElementsByTag("th");
            for (Element tableHeaderValue:tableHeaderValues) {
                headers.add(tableHeaderValue);
            }
            //excludeFirstElement(tableRows);//First element has the Table Headings, not an actual row
            parseRows(tableRows);
        } else {
            System.out.println("empty header names");
            parseRows(tableRows);
        }
    }
}
