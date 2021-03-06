package org.electrocodeogram.module.intermediate.implementation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.electrocodeogram.event.CommonData;
import org.electrocodeogram.event.IllegalEventParameterException;
import org.electrocodeogram.event.MicroActivity;
import org.electrocodeogram.event.ValidEventPacket;
import org.electrocodeogram.event.WellFormedEventPacket;
import org.electrocodeogram.logging.LogHelper;
import org.electrocodeogram.logging.LogHelper.ECGLevel;
import org.electrocodeogram.misc.xml.ECGParser;
import org.electrocodeogram.misc.xml.NodeException;
import org.electrocodeogram.module.intermediate.IntermediateModule;
import org.electrocodeogram.module.intermediate.implementation.location.change.BlockChange;
import org.electrocodeogram.module.intermediate.implementation.location.change.History;
import org.electrocodeogram.module.intermediate.implementation.location.change.LineChange;
import org.electrocodeogram.module.intermediate.implementation.location.change.LocationChange;
import org.electrocodeogram.module.intermediate.implementation.location.change.BlockChange.BlockChangeType;
import org.electrocodeogram.module.intermediate.implementation.location.change.LineChange.LineChangeType;
import org.electrocodeogram.module.intermediate.implementation.location.change.LocationChange.LocationChangeType;
import org.electrocodeogram.module.intermediate.implementation.location.state.Line;
import org.electrocodeogram.module.intermediate.implementation.location.state.Location;
import org.electrocodeogram.module.intermediate.implementation.location.state.LocationComputationStrategy;
import org.electrocodeogram.module.intermediate.implementation.location.state.Text;
import org.electrocodeogram.modulepackage.ModuleProperty;
import org.electrocodeogram.modulepackage.ModulePropertyException;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;

/**
 * Module to assemble (code) location with retained identity over time and
 * send location change events.
 */
public class CodeLocationTrackerIntermediateModule extends IntermediateModule {

    private static final String CREATOR_STRING = "CodeLocationTrackerIntermediateModule1.1.4";

    private int ccc = 0; // For Debugging purposes: A counter of the calls to analyse

    /**
     * The class logger
     */
    public static Logger logger = LogHelper 
        .createLogger(CodeLocationTrackerIntermediateModule.class.getName());

    /**
     * Holds list of texts. A Text contains the <b>actual</b> (accouring to analysed events)
     * state of a document which is its <i>Location</i>s and <i>Line</i>s. This variable maps
     * the documentname to its Text object. 
     */
    private HashMap<String, Text> texts = new HashMap<String, Text>();

    /**
     * Holds for each Location a list of LocationChanges and for each block change the location changes
     * 
     * TODO Maybe not useful in later versions, because the history can be compiled 
     * from a BlockChange history
     */
    private History history = new History();
    
    /**
     * Holds methods to (re-)compute Line and Location instances and properties  
     */
    private LocationComputationStrategy strategy = new LocationComputationStrategy();
    
	/**
     * Standard Intermediate Module constructor
     * 
	 * @param id
	 * @param name
	 */
	public CodeLocationTrackerIntermediateModule(String id, String name) {
		super(id, name);        
	}

	/**
	 * @see org.electrocodeogram.module.intermediate.IntermediateModule#analyse(org.electrocodeogram.event.ValidEventPacket)
	 */
	public Collection<ValidEventPacket> analyse(ValidEventPacket eventPacket) {

//System.out.println(eventPacket);
        ccc++;
//System.out.println("Event No. " + ccc + " of type " + eventPacket.getSensorDataType() + " from " + eventPacket.getTimeStamp());        
        // a list of location changes which will be sent as event packets
        List<LocationChange> locChanges = new ArrayList<LocationChange>();

        if (eventPacket.getMicroSensorDataType().getName().equals("msdt.linediffbase.xsd")) {
            // On line diff base events compile first set of locations and initial location changes
    		
            String documentName = getDocumentName(eventPacket); 
    		if (!documentName.endsWith(".java")) // TODO currently only for .java documents
    			return null;
            String id = getId(eventPacket);
            if (id == null || id.length() == 0)
                id = documentName;

            String code = getCode(eventPacket);
            String projectName = getProjectname(eventPacket);

            Text text = texts.get(projectName + ":" + documentName);
            if (text != null) {
                // This means a linediffbase has already been sent some time ago
                // The CodeChangeDiffer treats this just like a code change, so do we
                // LineDiffs must have been send right before this one 
                assert (text.printContents().trim().equals(code.trim()));
                // A new linediffbase isn't of any interest
                return null;  
            }

            text = new Text(documentName);            
            String[] lines = getLines(code);
    		
            // Prepare initial block change
            BlockChange blockChange = new BlockChange(text, 
                    eventPacket.getTimeStamp(), BlockChangeType.CREATED, 0, lines.length);
            history.getBlockChangeHistory().add(blockChange);

            // Create initial lines
            Line newLine = null;
    		List<Line> newLines = new ArrayList<Line>(lines.length);
            for (int lineNo = 0; lineNo < lines.length; lineNo++) {
                Line prevLine = newLine;
                newLine = new Line(lineNo, lines[lineNo]);
                blockChange.getLineChanges().add(
                        new LineChange(LineChangeType.INSERTED, lineNo, "", lines[lineNo]));
    			strategy.computeBasicLineProperties(newLine);
                strategy.computeCohesionAndLevel(newLine, prevLine);
                newLines.add(lineNo, newLine);
    		}
            text.setLines(newLines);
            
    		// Compute initial locations, register them and report location changes
            Collection<Location> newLocs = strategy.computeLocations(newLines);
            
            // Add locations
            for (Location newLoc : newLocs) {
                text.addLocation(newLoc);
                LocationChange lc = new LocationChange(newLoc, 
                        LocationChangeType.INITIATED, -1);
                history.addLocationChange(lc, blockChange);
                locChanges.add(lc);
            }
/*    
System.out.println("First locations computation on file " + documentName);
System.out.println("at " + eventPacket.getTimeStamp() + ":");
System.out.println(text.printLocations());
System.out.println("---");
*/
            this.texts.put(projectName + ":" + documentName, text);
            
            assert (text.checkValidity());
            assert (text.printContents().trim().equals(code.trim()));
            assert (history.checkValidity(text));
        }
        
        else if (eventPacket.getMicroSensorDataType().getName().equals("msdt.linediff.xsd")) {

            String documentName = getDocumentName(eventPacket); 
            if (!documentName.endsWith(".java"))
                return null;
            String id = getId(eventPacket);
            if (id == null || id.length() == 0)
                id = documentName;

            String projectName = getProjectname(eventPacket);

            Text text = texts.get(projectName + ":" + documentName);
            assert(text != null);            
            
            if (text == null) {
                // A linediffbase seem to be missing. This should not occur, but may happen
                //   due to incomplete events. We assume this document has been empty before
                text = new Text(documentName);
                texts.put(projectName + ":" + documentName, text);
            }
            
            Collection<BlockChange> blockChanges = BlockChange.parseLineDiffsEvent(text, eventPacket);            
//System.out.println(blockChanges.size());
            
            for (BlockChange bc: blockChanges) {

                history.addBlockChange(bc);
                
                // Convert long change blocks into replace blocks. This is due to the fact that
                //   its more likely that is an paste/overwrite block (i.e. complete replace)
                //   than its a change of the lines, i.e. gradually changing each line individually
                // TODO This needs more check: Check prevoious CCP events, and edit distance of changed lines
                // long = more than 2
                if (bc.getBlockType() == BlockChangeType.CHANGED && bc.getBlockLength() > 2)
                    bc.setBlockType(BlockChangeType.REPLACED);
                // replace will be processed as a delete followed by an insert
                // TODO mostly, paste-replaces are combined with insertions and deletions because
                //   the pasted block is often shorter or longer than the replaced one. currently
                //   this results in a replace foloowed or preceeded by an delete or insert block
                //   although it actually is a delete block followed by an insert block of
                //   different length
                
//System.out.println(bc);

                BlockChangeType type = bc.getBlockType();
                
                if (type == BlockChangeType.DELETED || type == BlockChangeType.REPLACED) {
                    
                    // Get block start
                    Line startLine = text.getLine(bc.getBlockStart());
                    Location location = startLine.getLocation();
                    Collection<Location> tail = text.getLastLocations(location); // affected locations
                    Iterator<Location> tailIt = tail.iterator();
                    location = tailIt.next(); // at least on element (=location) must be present
                    List<LocationChange> newLocChanges = new ArrayList<LocationChange>(); // temporarely stores new generated LocChanges
                    
                    // The location may contain the deleted block completely
                    if (location.getStart() < bc.getBlockStart() && location.getEnd() > bc.getBlockEnd()) {
                        location.setLength(location.getLength() - bc.getBlockLength());
                        newLocChanges.add(new LocationChange(location, LocationChangeType.SHORTENED_IN_BETWEEN, -1));                        
                        location = (tailIt.hasNext() ? tailIt.next() : null);
                    }
                    else // ok, seems to be more complicated: check three stages 
                    {                     
                        // The first location may be only truncated at the end
                        if (location.getStart() < bc.getBlockStart() && location.getEnd() >= bc.getBlockStart()) {
                            // the latter few lines will be deleted
                            location.setLength(bc.getBlockStart() - location.getStart());
                            newLocChanges.add(new LocationChange(location, LocationChangeType.SHORTENED_AT_END, -1));
                            location = (tailIt.hasNext() ? tailIt.next() : null);
                        }
                        
                        // The next few locations which completely cover the deleted lines will be emptied
                        while (location != null && location.getStart() >= bc.getBlockStart() && location.getEnd() <= bc.getBlockEnd()) {
                            location.setLength(0); // set to 0 to correctly recompute the contents
                            newLocChanges.add(new LocationChange(location, LocationChangeType.SHORTENED_AT_ALL, -1));
                            tailIt.remove(); // 1. remove will be refelcted in text.getLocations(), 2. Really remove? Yes, because of the ordering in text
                            location = (tailIt.hasNext() ? tailIt.next() : null);
                        }
                        
                        // The next partly covered location will be shortend in the start
                        if (location != null && location.getStart() <= bc.getBlockEnd() && location.getEnd() > bc.getBlockEnd()) {
                            location.setLength(location.getEnd() - bc.getBlockEnd());
                            location.setStart(bc.getBlockStart());
                            newLocChanges.add(new LocationChange(location, LocationChangeType.SHORTENED_AT_START, -1));
                            location = (tailIt.hasNext() ? tailIt.next() : null);
                        }
                    }
                    
                    // The locations after the deleted block needs adjustment in their start line number
                    while (location != null) {
                        location.setStart(location.getStart() - bc.getBlockLength());
                        location = (tailIt.hasNext() ? tailIt.next() : null);
                    }

                    // As well, the lines themselves after the deleted lines need adjustment in their line numbers
                    for (Line l : text.getLastLines(bc.getBlockEnd() + 1)) { 
                        l.setLinenumber(l.getLinenumber() - bc.getBlockLength());
                    }
                    
                    // Now, actually delete the lines in the text
                    for (int i = 0; i < bc.getBlockLength(); i++)
                        text.deleteLine(bc.getBlockStart()); // it's always the first line number!
                    
                    // Register location change
                    for (LocationChange locChange : newLocChanges) {
                        // Will recompute the contents (necessary because only now the lines are corrrectly deleted)
                        history.addLocationChange(locChange, bc);
                        locChanges.add(locChange);
                    }
                    
                    // Finally, the new neighborhood needs to be analysed. bc.blockStart now
                    // points to the first line *after* the deleted block
                    if (type != BlockChangeType.REPLACED) {
                        // In case of a replace, it will be followed by an insert at this place anyway
                        Line prevLine = text.getLine(bc.getBlockStart()-1); // maybe null
                        Line nextLine = text.getLine(bc.getBlockStart()); // maybe null
                        strategy.computeNewNeighborhood(text, history, prevLine, nextLine, bc);
                    }

                }
                
                if (type == BlockChangeType.INSERTED || type == BlockChangeType.REPLACED) {

                    // Compute the line just below the first new one. Note that "insert at line x"
                    //   means that x is the line number of the first inserted line, and that the
                    //   prior line x will be moved below the block, i.e. to line x + bc.blockLength
                    LocationChange locChangedAbove = null, locChangedBelow = null, 
                                    locForked = null, locSplit = null, 
                                    locMergedAway = null, locMergedAdd = null; // some possible changes
                    Line insertLineBelow = text.getLine(bc.getBlockStart());
                    Location insertLocBelow = null; 
                    if (insertLineBelow != null)
                        insertLocBelow = insertLineBelow.getLocation();
                    //else 
                    //    insertLocBelow = text.getLastLocation(); // get last location if inserted at end 
                    
                    // fetch location just above insertion line
                    Line insertLineAbove = text.getLine(bc.getBlockStart()-1);
                    Location insertLocAbove = null;
                    if (insertLineBelow == null) { 
                        // insertLocBelow/insertLineBelow == null means, inserted block at end of text, take last loc above
                        insertLocAbove = text.getLastLocation();
                    } else if (insertLineAbove == null) {
                        // was insertion at line 0, so there's no above
                        insertLocAbove = null;
                    } else if (insertLineAbove.getLocation() != insertLocBelow ) { 
                        // insertLineBelow is at the beginning of the insertLoc. then take prev location
                        insertLocAbove = insertLineAbove.getLocation();
                    } else {
                        // Temporarly split the insertLocation at the insert position
                        insertLocAbove = insertLocBelow.splitLocation(insertLineBelow);
                        // register new location temporaly
                        text.addLocation(insertLocAbove);
                        locForked = new LocationChange(insertLocAbove, 
                                LocationChangeType.SPLIT_ADD_AT_END, insertLocBelow.getId());
                        locSplit = new LocationChange(insertLocBelow, 
                                LocationChangeType.SPLIT_DEL_AT_START, insertLocAbove.getId());
                        // Exchange LocAbove and LocBelow in case of wrong order due to split decision
                        if (insertLocAbove.getStart() > insertLocBelow.getStart()) {
                            Location tmp = insertLocAbove;
                            insertLocAbove = insertLocBelow;
                            insertLocBelow = tmp;
                            locSplit.setType(LocationChangeType.SPLIT_DEL_AT_END);
                            locForked.setType(LocationChangeType.SPLIT_ADD_AT_START);
                        }
                    }
                    // retain location sizes for later comparison
                    int insertLocAboveSize = (insertLocAbove != null ? insertLocAbove.getLength() : 0);
                    int insertLocBelowSize = (insertLocBelow != null ? insertLocBelow.getLength() : 0);
                    
                    // Create Collection of new lines 
                    Line[] newLines = new Line[bc.getBlockLength()];
                    int lineOffset = 0;
                    Line prevLine = insertLineAbove;
                    while (lineOffset < bc.getBlockLength()) {
                        int linenumber = bc.getBlockStart() + lineOffset;
                        LineChange lineChange = bc.getLineChanges().get(lineOffset);
                        Line newLine = new Line(linenumber, lineChange.getContents());
                        strategy.computeBasicLineProperties(newLine);
                        strategy.computeCohesionAndLevel(newLine, prevLine);
                        newLines[lineOffset] = newLine;
                        text.insertLine(linenumber, newLine);
                        prevLine = newLine;
                        lineOffset++;
                    }
                    // precompute cohesion of following line for better debugging
                    if (insertLineBelow != null)
                        strategy.computeCohesionAndLevel(insertLineBelow, newLines[newLines.length-1]);

                    // Compute Locations in the block of new lines
                    Collection<Location> newLocs = strategy.computeLocations(Arrays.asList(newLines));
                    
                    // change following line numbers and location
                    int afterBlock = bc.getBlockEnd()+1;
                    for (Line lin : text.getLastLines(afterBlock)) {
                        lin.setLinenumber(lin.getLinenumber() + bc.getBlockLength());
                    } 
                    if (insertLocBelow != null) {
                        for (Location loc : text.getLastLocations(insertLocBelow)) {
                            loc.setStart(loc.getStart() + bc.getBlockLength());
                        }
                    }

                    // now check start and end for possible merges, at start and at end
                    // start: check cohesion of first line of the block with line at insert position (insertLine).
                    if (insertLocAbove != null) { // the may be no above, than skip
                        Line firstBlockLine = newLines[0];
                        // note: the new cohesion has already been computed
                        if (firstBlockLine.getCohesion() >= Location.MIN_COHESION) {
                            // merge the new location with the old one
                            Location firstBlockLocation = firstBlockLine.getLocation();
                            newLocs.remove(firstBlockLocation); // remove first computes loc since it was merged away
                            insertLocAbove.mergeLocation(firstBlockLocation); // merge locs
                            // report the change of the insertLoc
                            locChangedAbove = new LocationChange(insertLocAbove, 
                                    LocationChangeType.EXTENDED_AT_END, -1);
                        }
                    }
                    // end: check cohesion at end, if low merge them.
                    if (insertLocBelow != null) { // the may be no below, than skip
                        Line lastBlockLine = newLines[newLines.length-1];
                        // note: the new cohesion has already been computed
                        if (insertLineBelow.getCohesion() >= Location.MIN_COHESION) {
                            Location lastBlockLocation = lastBlockLine.getLocation();
                            // We have three special cases
                            if (locSplit == null && insertLocAbove == lastBlockLocation) {
                                // This is a special case: The new block (which generated 
                                //   only one new location), merges together with previously distinct
                                //   locations insertLocAbove and insertLocBelow
                                // What already happened: insertLocAbove has been merged with the new block
                                // Now decide which old location should win (the previously bigger one)
                                assert (locChangedAbove.getLocation() == insertLocAbove);
                                if (insertLocAboveSize >= insertLocBelowSize) {
                                    text.removeLocation(insertLocBelow);
                                    insertLocAbove.mergeLocation(insertLocBelow);
                                    locMergedAway = new LocationChange(insertLocBelow, 
                                            LocationChangeType.MERGED_DEL_AT_START, insertLocAbove.getId());
                                    locMergedAdd = new LocationChange(insertLocAbove, 
                                            LocationChangeType.MERGED_ADD_AT_END, insertLocBelow.getId());
                                    locChangedAbove.setRelatedLocId(insertLocBelow.getId());
                                } else {
                                    text.removeLocation(insertLocAbove);
                                    insertLocBelow.mergeLocation(insertLocAbove);
                                    locMergedAway = new LocationChange(insertLocAbove, 
                                            LocationChangeType.MERGED_DEL_AT_END, insertLocBelow.getId());
                                    locMergedAdd = new LocationChange(insertLocBelow, 
                                            LocationChangeType.MERGED_ADD_AT_START, insertLocAbove.getId());
                                    // overwrite locChangedAbove event for insertLocBelow, locChnagedBelow will be ignored
                                    locChangedAbove = new LocationChange(insertLocBelow, 
                                            LocationChangeType.EXTENDED_AT_START, -1);
                                }
                            }
                            else if (locSplit != null && insertLocAbove == lastBlockLocation) {
                                // The second case is where insertLocBelow/Above has been split from
                                //   insertLocAbove/Below and is now recombined together with the
                                //   inserted block (consisting of only one location)
                                // What already happened: insertLocAbove has been merged with the new block
                                // Now decide which was the original location, abandon the split one and merge them all
                                Location forkedLocation = locForked.getLocation();
                                if (forkedLocation == insertLocBelow) { // abandon insertLocBelow
                                    text.removeLocation(insertLocBelow); // has already been registered
                                    insertLocAbove.mergeLocation(insertLocBelow);
                                    locSplit = null; // act as like split has never been occured
                                    locForked = null;
                                    // set changed type to IN_BETWEEN, the rest has already been generated
                                    locChangedAbove.setType(LocationChangeType.EXTENDED_IN_BETWEEN);
                                } else {
                                    text.removeLocation(insertLocAbove);
                                    insertLocBelow.mergeLocation(insertLocAbove);
                                    locSplit = null; // act as like split has never been occured
                                    locForked = null;
                                    // overwrite locChangedAbove event for insertLocBelow, locChnagedBelow will be ignored
                                    locChangedAbove = new LocationChange(insertLocBelow, 
                                            LocationChangeType.EXTENDED_IN_BETWEEN, -1);
                                }
                            }
                            else {
                                // The third and last case the the most general one: either the block
                                //   introduced more than one location, or the cohesion above or
                                //   below don't both lead the merges
                                newLocs.remove(lastBlockLocation); // remove last block loc since it was merged away
                                // merge the new location with the old one just like it was done in the firstBlockLocation case
                                insertLocBelow.mergeLocation(lastBlockLocation);
                                // report the change of the insertLoc
                                locChangedBelow = new LocationChange(insertLocBelow, 
                                        LocationChangeType.EXTENDED_AT_START, -1);
                            }                                    
                        }
                    }
                    // Ok, now there may be some overlapping location changes. Minimize them and report them
                    // using the latest location contents
                    if (locForked != null) {
                        // check overlapping events (i.e. ADDED/FORKED + {ANYTHING} should become ADDED/FORKED only)
                        if (locChangedAbove != null && locChangedAbove.getLocation() == locForked.getLocation())
                            locChangedAbove = null;
                        if (locChangedBelow != null && locChangedBelow.getLocation() == locForked.getLocation())
                            locChangedBelow = null;
                        if (locSplit != null && locSplit.getLocation() == locForked.getLocation())
                            locSplit = null;
                    }
                    history.addLocationChange(locForked, bc);
                    history.addLocationChange(locSplit, bc);
                    history.addLocationChange(locChangedAbove, bc);
                    history.addLocationChange(locChangedBelow, bc);
                    history.addLocationChange(locMergedAway, bc);
                    history.addLocationChange(locMergedAdd, bc);
                    if (locForked != null) locChanges.add(locForked);
                    if (locSplit != null) locChanges.add(locSplit);
                    if (locChangedAbove != null) locChanges.add(locChangedAbove);
                    if (locChangedBelow != null) locChanges.add(locChangedBelow);
                    if (locMergedAway != null) locChanges.add(locMergedAway);
                    if (locMergedAdd != null) locChanges.add(locMergedAdd);
                    
                    // register remaining new locations in text and report location changes 
                    for (Location newLoc : newLocs) {
                        text.addLocation(newLoc);
                        LocationChange lc = new LocationChange(newLoc, LocationChangeType.ADDED, -1);
                        history.addLocationChange(lc, bc);
                        locChanges.add(lc);
                    }

                }
                
                if (type == BlockChangeType.CHANGED) {
                    
                    // Change blocks are processed line by line
                    // TODO should be a block operation as well, but that's not easy. I don't know
                    //   of any algorithm which would result in a much different procedere like
                    //   liny-by-line processing. Current- and additionally, long insert blocks
                    //   are processed as replace blocks, so insert blocks will be small anyway.
                    for (int lineOffset = 0; lineOffset < bc.getBlockLength(); lineOffset++) {
    
                        // first get the affected Location and its Line
                        int linenumber = bc.getBlockStart() + lineOffset;
                        LineChange lineChange = bc.getLineChanges().get(lineOffset);
                        Line line = text.getLine(linenumber);
                        Location location = line.getLocation();
                        assert (location != null);
                        assert (line != null);
                        
                        // secondly perfom the linediff operation in the text
                        line.setContents(lineChange.getContents());
                        strategy.computeBasicLineProperties(line);
                        LocationChange lc = new LocationChange(location, LocationChangeType.CHANGED, -1);
                        history.addLocationChange(lc, bc);
                        locChanges.add(lc);
        
                        // thirdly perform recomputation of Locations at the new line neighborhoods
                        Line prevLine = text.getLine(linenumber-1); // maybe null
                        Line nextLine = text.getLine(linenumber+1); // maybe nulla
                        strategy.computeNewNeighborhood(text, history, prevLine, line, bc);
                        strategy.computeNewNeighborhood(text, history, line, nextLine, bc);
                        
                    }
                    
                } 
//System.out.println(history.printHistoryTextComparison(text));
                assert (history.checkValidity(text));
                assert (text.checkValidity());
                assert (text.printContents().equals(text.printContentsFromLocations()));
                assert (text.printContents().equals(history.printLastTextContents(text)));

            }
            
//System.out.println("Codechange No. " + (++ccc) + " at time: " + eventPacket.getTimeStamp());
//System.out.println(text);
//System.out.println("---------------------------------------------------------");
          
        }

		// TODO just for assertions
        else if (eventPacket.getMicroSensorDataType().getName().equals("msdt.codechange.xsd")) {
            
            // Currently codechages are filtered by the CodeChangeDiffer
            String documentName = getDocumentName(eventPacket); 
            if (!documentName.endsWith(".java"))
                return null;
            String id = getId(eventPacket);
            if (id == null || id.length() == 0)
                id = documentName;

            String projectName = getProjectname(eventPacket);

            Text text = texts.get(projectName + ":" + documentName);
            assert (text != null);

            String code = getCode(eventPacket);
            assert (text.printContents().trim().equals(code.trim()));
        }

        // TODO just for debugging
        else if (eventPacket.getMicroSensorDataType().getName().equals("msdt.system.xsd")) {
            try {
                if (ECGParser.getSingleNodeValue("type", eventPacket.getDocument()).equals("termination")) {
                    
//System.out.println(history.printLocationChangeHistory());
//System.out.println(history.printBlockChangeHistory());

                }
            } catch (NodeException e) {
                // TODO report this
                e.printStackTrace();
                return null;
            }
        }

        // Finally send the collected location changes as events
        List<ValidEventPacket> events = null; 
        if (locChanges.size() > 0) {
            events = new ArrayList<ValidEventPacket>();
            for (LocationChange lc : locChanges) {
                String data = lc.asEventString(CREATOR_STRING, 
                        getId(eventPacket), 
                        getProjectname(eventPacket), 
                        getUsername(eventPacket));
                String[] args = {WellFormedEventPacket.HACKYSTAT_ADD_COMMAND, 
                                "msdt.codelocation.xsd", 
                                data};
                try {
                    events.add(new ValidEventPacket(eventPacket.getTimeStamp(),
                        WellFormedEventPacket.HACKYSTAT_ACTIVITY_STRING, Arrays.asList(args)));
                } catch (IllegalEventParameterException e) {
                    logger.log(ECGLevel.SEVERE, "Wrong event parameters in CodeLocationTrackerIntermediateModule.");
                }
            }
        }
        
        return events;
        
	}

    private String getDocumentName(ValidEventPacket packet) {
    	try {
        	Document document = packet.getDocument();
            String documentname = ECGParser.getSingleNodeValue("documentname", document);
            if (documentname == null)
                return "UNKNOWN";
            return documentname;
    	} catch (NodeException e) {
			logger.log(ECGLevel.SEVERE, "Could not fetch documentname in CodeLocationTrackerIntermediateModule.");
            // Actually there has been a bug in older EclipseSensors which results in empty document names
			return "UNKNOWN";
		}
	}

	private String getCode(ValidEventPacket packet) {
    	try {
        	Document document = packet.getDocument();
            String code = ECGParser.getSingleNodeValue("document", document);
            if (code == null)
                return "";
            return code;
    	} catch (NodeException e) {
			logger.log(ECGLevel.SEVERE, "Could not fetch code in CodeLocationTrackerIntermediateModule.");
            assert (false);
			return null;
		}
    }

    private String getId(ValidEventPacket packet) {
        try {
            Document document = packet.getDocument();
            return ECGParser.getSingleNodeValue("id", document);
        } catch (NodeException e) {
            // ignore it
            return null;
        }
    }

    private String getUsername(ValidEventPacket packet) {
        try {
            Document document = packet.getDocument();
            return ECGParser.getSingleNodeValue("username", document);
        } catch (NodeException e) {
            logger.log(ECGLevel.SEVERE, "Could not fetch username in CodeLocationTrackerIntermediateModule.");
            assert (false);
            return null;
        }
    }

    private String getProjectname(ValidEventPacket packet) {
        try {
            Document document = packet.getDocument();
            return ECGParser.getSingleNodeValue("projectname", document);
        } catch (NodeException e) {
            logger.log(ECGLevel.SEVERE, "Could not fetch projectname in CodeLocationTrackerIntermediateModule.");
            assert (false);
            return null;
        }
    }

    /**
     * Splits a file contents into an array of lines
     * 
     * @param contents A text file contents
     * @return The single lines (seperated by \n) in code
     */
    private String[] getLines(String contents) {
        if (contents == null) {
            return new String[0];
        }
        StringBuffer buffer=new StringBuffer();
        Vector<String> res=new Vector<String>();
        int size=contents.length();
        for (int index=0;index<size;index++) {
           char current=contents.charAt(index);
           if (current==0x0D) {
              if (index+1<size && contents.charAt(index+1)==0x0A)
                  index++;
              res.add(buffer.toString());
              buffer.setLength(0);
           } else if (current==0x0A) {
              if (index+1<size && contents.charAt(index+1)==0x0D)
                  index++;
              res.add(buffer.toString());
              buffer.setLength(0);
           } else buffer.append(current);
        }
        if (buffer.length()>0)
           res.add(buffer.toString());
        return (String[])res.toArray(new String[0]);
    }

	public void initialize() {
		this.setProcessingMode(ProcessingMode.ANNOTATOR);
	}

	protected void propertyChanged(ModuleProperty moduleProperty)
			throws ModulePropertyException {
		// no properties
	}

	public void update() {
		// no properties
	}

}
