/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package test.org.helios.google.ss;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gdata.client.spreadsheet.FeedURLFactory;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.Link;
import com.google.gdata.data.batch.BatchOperationType;
import com.google.gdata.data.batch.BatchStatus;
import com.google.gdata.data.batch.BatchUtils;
import com.google.gdata.data.spreadsheet.Cell;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;


/**
 * <p>Title: BatchUpdater</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>test.org.helios.google.ss.BatchUpdater</code></p>
 */

public class BatchUpdater {
	 /** The number of rows to fill in the destination workbook */
	  private static final int MAX_ROWS = 75;

	  /** The number of columns to fill in the destination workbook */
	  private static final int MAX_COLS = 5;

	  /**
	   * A basic struct to store cell row/column information and the associated RnCn
	   * identifier.
	   */
	  private static class CellAddress {
	    public final int row;
	    public final int col;
	    public final String idString;

	    /**
	     * Constructs a CellAddress representing the specified {@code row} and
	     * {@code col}.  The idString will be set in 'RnCn' notation.
	     */
	    public CellAddress(int row, int col) {
	      this.row = row;
	      this.col = col;
	      this.idString = String.format("R%sC%s", row, col);
	    }
	  }

	  public static void main(String[] args) throws AuthenticationException, MalformedURLException, IOException, ServiceException {

	    String username = "nwhitehead@heliosdev.org";
	    String password = "jer1029";
	    String key = "t7jt7V4egGWIpFl5Iv4jM3Q";
	    

//	    if (help || username == null || password == null) {
//	      System.err.print("Usage: java BatchCellUpdater --username [user] --password [pass] --key [spreadsheet-key]\n\n");
//	      System.exit(1);
//	    }
	    SpreadsheetService ssSvc = new SpreadsheetService("Batch Cell Demo");
	    ssSvc.setUserCredentials(username, password);
	    ssSvc.setProtocolVersion(SpreadsheetService.Versions.V3);
	    
	    URL metafeedUrl = new URL("https://spreadsheets.google.com/feeds/spreadsheets/private/full");
	    SpreadsheetFeed feed = ssSvc.getFeed(metafeedUrl, SpreadsheetFeed.class);
	    List<SpreadsheetEntry> spreadsheets = feed.getEntries();
	    for (int i = 0; i < spreadsheets.size(); i++) {
	      SpreadsheetEntry entry = spreadsheets.get(i);
	      
	      System.out.println("\t" + entry.getTitle().getPlainText() + "  [" + entry.getKey() + "]");
	    }	    

	    long startTime = System.currentTimeMillis();

	    // Prepare Spreadsheet Service
//	    SpreadsheetService ssSvc = new SpreadsheetService("Batch Cell Demo");

	    FeedURLFactory urlFactory = FeedURLFactory.getDefault();
	    URL cellFeedUrl = urlFactory.getCellFeedUrl(key, "od6", "private", "full");
	    System.out.println("Cell Feed URL [" + cellFeedUrl + "]");
	    CellFeed cellFeed = ssSvc.getFeed(cellFeedUrl, CellFeed.class);

	    // Build list of cell addresses to be filled in
	    List<CellAddress> cellAddrs = new ArrayList();
	    for (int row = 1; row <= MAX_ROWS; ++row) {
	      for (int col = 1; col <= MAX_COLS; ++col) {
	        cellAddrs.add(new CellAddress(row, col));
	      }
	    }

	    // Prepare the update
	    // getCellEntryMap is what makes the update fast.
	    Map cellEntries = getCellEntryMap(ssSvc, cellFeedUrl, cellAddrs);

	    CellFeed batchRequest = new CellFeed();
	    for (CellAddress cellAddr : cellAddrs) {
	      URL entryUrl = new URL(cellFeedUrl.toString() + "/" + cellAddr.idString);
	      //System.out.println("Cell Entry:[" + cellEntries.get(cellAddr.idString).getClass().getName() + "] " + cellEntries.get(cellAddr.idString));
	      CellEntry batchEntry = (CellEntry)cellEntries.get(cellAddr.idString);
	      batchEntry.changeInputValueLocal(cellAddr.idString);
	      BatchUtils.setBatchId(batchEntry, cellAddr.idString);
	      BatchUtils.setBatchOperationType(batchEntry, BatchOperationType.UPDATE);
	      batchRequest.getEntries().add(batchEntry);
	    }

	    // Submit the update
	    Link batchLink = cellFeed.getLink(Link.Rel.FEED_BATCH, Link.Type.ATOM);
	    CellFeed batchResponse = ssSvc.batch(new URL(batchLink.getHref()), batchRequest);

	    // Check the results
	    boolean isSuccess = true;
	    for (CellEntry entry : batchResponse.getEntries()) {
	      String batchId = BatchUtils.getBatchId(entry);
	      if (!BatchUtils.isSuccess(entry)) {
	        isSuccess = false;
	        BatchStatus status = BatchUtils.getBatchStatus(entry);
	        System.out.printf("%s failed (%s) %s", batchId, status.getReason(), status.getContent());
	      }
	    }

	    System.out.println(isSuccess ? "\nBatch operations successful." : "\nBatch operations failed");
	    System.out.printf("\n%s ms elapsed\n", System.currentTimeMillis() - startTime);
	  }

	  /**
	   * Connects to the specified {@link SpreadsheetService} and uses a batch
	   * request to retrieve a {@link CellEntry} for each cell enumerated in {@code
	   * cellAddrs}. Each cell entry is placed into a map keyed by its RnCn
	   * identifier.
	   *
	   * @param ssSvc the spreadsheet service to use.
	   * @param cellFeedUrl url of the cell feed.
	   * @param cellAddrs list of cell addresses to be retrieved.
	   * @return a map consisting of one {@link CellEntry} for each address in {@code
	   *         cellAddrs}
	   */
	  public static Map getCellEntryMap(
	      SpreadsheetService ssSvc, URL cellFeedUrl, List<CellAddress> cellAddrs)
	      throws IOException, ServiceException {
	    CellFeed batchRequest = new CellFeed();
	    for (CellAddress cellId : cellAddrs) {
	      CellEntry batchEntry = new CellEntry(cellId.row, cellId.col, cellId.idString);
	      batchEntry.setId(String.format("%s/%s", cellFeedUrl.toString(), cellId.idString));
	      BatchUtils.setBatchId(batchEntry, cellId.idString);
	      BatchUtils.setBatchOperationType(batchEntry, BatchOperationType.QUERY);
	      batchRequest.getEntries().add(batchEntry);
	    }

	    CellFeed cellFeed = ssSvc.getFeed(cellFeedUrl, CellFeed.class);
	    CellFeed queryBatchResponse =
	      ssSvc.batch(new URL(cellFeed.getLink(Link.Rel.FEED_BATCH, Link.Type.ATOM).getHref()),
	                  batchRequest);

	    Map cellEntryMap = new HashMap(cellAddrs.size());
	    for (CellEntry entry : queryBatchResponse.getEntries()) {
	      cellEntryMap.put(BatchUtils.getBatchId(entry), entry);
	      System.out.printf("batch %s {CellEntry: id=%s editLink=%s inputValue=%s\n",
	          BatchUtils.getBatchId(entry), entry.getId(), entry.getEditLink().getHref(),
	          entry.getCell().getInputValue());
	    }

	    return cellEntryMap;
	  }

}
