package com.lucaszanella;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

/**
 * Simple HTML table parser to parse tables as Strings. According to https://developer.mozilla.org/en/docs/Web/HTML/Element/table,
 * has the following tags: <caption>, <colgroup>, <thead>, <tfoot>, <tbody>, <tr>, <th>, <td>. The tags <caption> and <colgroup>,
 * aren't critical, so we'll focus in the others. This library is intended for small tables, if you need to process big ones,
 * it may fill your RAM.
 * This library is originally written to UnespSisgradCrawler, so it may not support all table elements, it's just simple
 * functionality.
 */
public class jSoupTable {
    private Integer maxColumnNumber;
    private List<List<Tag>> rows = new ArrayList<>();

    /**
     * Returns the table headers, which are the elements in a row declared with <th>
     */
    /*
    public List<Element> getHeaders() {
        return this.headers;
    }

    public List<String> getHeaderElements() {
        List<String> headerNames = new ArrayList<>();
        for (Element header:this.headers) {
            headerNames.add(header.text());
        }
        return headerNames;
    }
    */
    /**
     * Returns all jSoup Element objects for this row, in a List. This option is useful
     * if you need to get the HTML formatted content of a row, not just the String.
     * For example, you'll receive a jSoup Element like this:
     * [<td class="myCssClass">row value 1</td>, <td>row value 2</td>, ...]
     */


    public List<Tag> getRowTags(int index) {
        return this.rows.get(index);
    }
    /**
     * Same thing but returns only the strings, not formatted HTML.
     */

    public List<String> getRowStrings(int index) {
        List<String> rowStrings = new ArrayList<>();
        //Since each row can have an arbitrary number of elements, we iterate through
        //each row, not through the headers size.
        for (Tag rowTag:getRowTags(index)) {
            rowStrings.add(rowTag.getTag().text());
        }
        return rowStrings;
    }
    /**
     * Same as getRowElements, but returns a list of all rows, not a specific row index
     */
    public List<List<Tag>> getAllRows() {
        return this.rows;
    }
    /**
     * Same as getAllRowElements, but for Strings
     */
    public List<List<String>>  getAllRowStrings() {
        List<List<String>> allRowStrings = new ArrayList<>();
        int rowsSize = getAllRows().size();//quantity of rows in this table
        for (int i = 0; i<rowsSize; i++) {
            allRowStrings.add(getRowStrings(i));
        }
        return allRowStrings;
    }
    public int getColumnIndex(String name, int headerIndex) {
        List<Tag> header = rows.get(headerIndex);
        //System.out.println("header is: "+header);
        //System.out.println("is header? "+header.get(0).isHeader());
        for (int i = 0; i<header.size(); i++) {
            //System.out.println(header.get(i).getTag().text());
            if (header.get(i).isHeader() && header.get(i).tag.text().toLowerCase().contains(name.toLowerCase())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Here we'll return true if there's at least one td element in this row.
     * I don't know why it'd be useful to have a mix of td and th tags in a row,
     * but I just need to know if this row is not a header-only.
     */
    public Boolean isRow(int index) {
        List<Tag> tags = getRowTags(index);
        for (Tag tag: tags) {
            if (tag.tagname.equals("td")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Instead of querying rows, we'll retrieve every Element belonging to a given Column.
     * Just to be used with tables that has a fixed column size. There can be tables with
     * variable column size, for example: 2 columns in the first row, 3 columns in the second.
     * These tables are mostly used to format HTML, besides being a bad technique. Since
     * the Sisgrad system uses fixed column size tables in some pages, I'm adding this.
     */
    public List<Tag> getColumnTags(int columnIndex) {
        List<Tag> columnValues = new ArrayList<>();
        for (List<Tag> row: rows) {
            columnValues.add(row.get(columnIndex));
        }
        return columnValues;
    }
    /**
     * Same as getColumnElements but for Strings, not HTML Element objects
     */
    public List<String> getColumnStrings(int columnIndex) {
        List<String> columnValues = new ArrayList<>();
        for (List<Tag> row: rows) {
            columnValues.add(row.get(columnIndex).getTag().text());
        }
        return columnValues;
    }
    /**
     * Same as getColumnElements, but returns a list of each column, where
     * each list contains the same as getColumnElements
     */
    /*
    public List<List<Element>> getAllColumnElements(int columnIndex) {
        List<List<Element>> columnElements = new ArrayList<>();
        int columnsSize = getHeaders().size();
        for (int i=0; i<columnsSize; i++) {
            columnElements.add(getColumnElements(i));
        }
        return columnElements;
    }
    */
    /**
     * Same as getAllColumnElements but for Strings, not HTML Element objects
     */
    public List<String> getAllColumnStrings(int columnIndex) {

        return null;
    }

    public class Tag {
        private String tagname;
        private Element tag;
        public Tag (Element tag) {
            this.tag = tag;
            this.tagname = tag.tagName();
        }
        public String getTagname() {
            return this.tagname;
        }
        public Element getTag() {
            return this.tag;
        }
        public Boolean isHeader() {
            if (this.tagname.equals("th")) {
                return true;
            } else {
                return false;
            }
        }
        public String toString() {
            return "name: " + tagname + ", Element: " + tag;
        }
    }

    /**
     * Since it's useful to store tables in sqlLite, and each table row can have an arbitrary number
     * of columns, we're gonna use numberOfColumns to keep track of each row's columns quantity. Then,
     * at the end, we take the max of this set to discover the max number of columns needed to store this
     * table in sqlLite, so we use this number to generate a dynamic sqlLite queries.
     */
    public jSoupTable(Element htmlTable) {
        //TODO: add support for <tfoot> and <tbody>
        //All rows of the table. We omit <caption> and <colgroup> tags. Method 'getElementsByTag' won't work with more than one tag

        //Every <thead>, <tbody> and <tfoot> has <tr> elements, so it's enough to pick just them.
        //If the <tr> has a <th> we'll consider it a header row, even though it can have a mix of <th> and <td> elements (why?).
        Elements tableRows = htmlTable.select("tr");
        //Keeps track of number of columns in each row, no order, because we want to take the max
        Set<Integer> numberOfColumns = new HashSet<>();
        //Add 0 so we won't get exception when taking Collections.max(); (at least one element must exist or it'll throw exception)
        numberOfColumns.add(0);
        for (Element rowElement:tableRows) {//iterates through each row
            List<Tag> rowTags = new ArrayList<>();

            Elements rowValues = rowElement.select("th, td");//<th> tags are header tags (which are column names), and <td> are header (or column values)
            numberOfColumns.add(rowValues.size());//adds the quantity of elements in this row, to numberOfColumns
            for (Element rowValue:rowValues) {//Iterates through rowElements to add each one to rowValuesList
                rowTags.add(new Tag(rowValue));//add these tags to the list of row tags
                //Elements rowElements = rowValue.select("th, td");
            }
            rows.add(rowTags);//adds the list of tags to the list of rows
        }
        this.maxColumnNumber = Collections.max(numberOfColumns);
    }
}