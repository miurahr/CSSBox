/*
 * BlockBox.java
 * Copyright (c) 2005-2007 Radek Burget
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * Created on 5. �nor 2006, 13:40
 */

package org.fit.cssbox.layout;

import java.awt.*;
import java.util.Iterator;
import java.util.Vector;

import org.w3c.dom.*;
import org.w3c.dom.css.CSSStyleDeclaration;

/**
 * A box corresponding to a block element
 *
 * @author  radek
 */
public class BlockBox extends ElementBox
{
    protected static final short FLOAT_NONE = 0;
    protected static final short FLOAT_LEFT = 1;
    protected static final short FLOAT_RIGHT = 2;
    
    protected static final short CLEAR_NONE = 0;
    protected static final short CLEAR_LEFT = 1;
    protected static final short CLEAR_RIGHT = 2;
    protected static final short CLEAR_BOTH = 3;
    
    protected static final short POS_STATIC = 0;
    protected static final short POS_RELATIVE = 1;
    protected static final short POS_ABSOLUTE = 2;
    protected static final short POS_FIXED = 3;
    
    protected static final short ALIGN_LEFT = 0;
    protected static final short ALIGN_RIGHT = 1;
    protected static final short ALIGN_CENTER = 2;
    protected static final short ALIGN_JUSTIFY = 3;
    
    protected static final short OVERFLOW_VISIBLE = 0;
    protected static final short OVERFLOW_HIDDEN = 1;
    protected static final short OVERFLOW_SCROLL = 2;
    protected static final short OVERFLOW_AUTO = 3;
    
    /** the minimal width of the space between the floating blocks that
     * can be used for placing the in-flow content */
    protected static final int INFLOW_SPACE_THRESHOLD = 20;
    
    /** Does this box contain blocks? */
    protected boolean contblock;
    
    /** Contains any in-flow boxes? */
    protected boolean anyinflow;
    
    /** Floating boxes left */
    protected FloatList fleft;
    
    /** Floating boxes right */
    protected FloatList fright;

    /** If we are a floating box, in which FloatList we are placed. */
    protected FloatList fown;

    /** x coordinate of the content box relative to the content box of the
     * top owner of the FloatList (from left)  */
    protected int floatXl;
    
    /** x coordinate of the content box relative to the content box of the
     * top owner of the FloatList (from right) */
    protected int floatXr;
    
    /** y coordinate of the content box relative to the content box of the 
        top owner of the FloatList */
    protected int floatY;
    
    /** Minimal size of the box. Values of -1 mean 'not set'. */
    protected Dimension min_size;
    
    /** Maximal size of the box. Values of -1 mean 'not set'. */
    protected Dimension max_size;
    
    //============================== Width computing ======================
    
    /** true if the box width has been set explicitly */
    protected boolean wset;
    
    /** true if the box height has been set explicitly */
    protected boolean hset;
    
    //TODO: add this to copy methods
    /** the preferred width or -1 if something is set to "auto" */
    protected int preferredWidth;
    
    /** Minimal width necessary for the last executed layout. */
    protected int lastPreferredWidth;
    
    //============================== Computed style ======================
    
    /** Floating property */
    protected short floating;
    
    /** Clearing property */
    protected short clearing;
    
    /** Position property */
    protected short position;
    
    /** Overflow property */
    protected short overflow;
        
    /** Position coordinates */
    protected LengthSet coords;
    
    /** The top position coordinate is set explicitely */
    protected boolean topset;
    
    /** The top position coordinate is set explicitely */
    protected boolean leftset;
    
    /** The top position coordinate is set explicitely */
    protected boolean bottomset;
    
    /** The top position coordinate is set explicitely */
    protected boolean rightset;
    
    /** Text-align property */
    protected short align;
    
    //=====================================================================
    
    /** Creates a new instance of BlockBox */
    public BlockBox(Element n, Graphics g, VisualContext ctx)
    {
        super(n, g, ctx);
        
        isblock = true;
        contblock = false;
        anyinflow = false;
        
        fleft = null;
        fright = null;
        fown = null;
        floatXl = 0;
        floatXr = 0;
        floatY = 0;
        
        floating = FLOAT_NONE;
        clearing = CLEAR_NONE;
        position = POS_STATIC;
        overflow = OVERFLOW_VISIBLE;
        align = ALIGN_LEFT;
        
        topset = false;
        leftset = false;
        bottomset = false;
        rightset = false;
        
        loadBlockStyle();
    }

    /** Convert an inline box to a block box */
    public BlockBox(InlineBox src)
    {
        super(src.el, src.g, src.ctx);
        setStyle(src.getStyle());
        
        cblock = src.cblock;
        isblock = true;
        contblock = false;
        anyinflow = false;
        style = src.style;
        
        fleft = null;
        fright = null;
        fown = null;
        floatXl = 0;
        floatXr = 0;
        floatY = 0;
        
        floating = FLOAT_NONE;
        clearing = CLEAR_NONE;
        position = POS_STATIC;
        overflow = OVERFLOW_VISIBLE;
        align = ALIGN_LEFT;

        topset = false;
        leftset = false;
        bottomset = false;
        rightset = false;
        
        nested = src.nested;
        startChild = src.startChild;
        endChild = src.endChild;
        loadBlockStyle();
    }
    
    public void copyValues(BlockBox src)
    {
        super.copyValues(src);
        contblock = src.contblock;
        anyinflow = src.anyinflow;
        setFloats(src.fleft, src.fright, src.floatXl, src.floatXr, src.floatY);
        fown = src.fown;
        floating = src.floating;
        clearing = src.clearing;
        position = src.position;
        overflow = src.overflow;
        align = src.align;
        topset = src.topset;
        leftset = src.leftset;
        bottomset = src.bottomset;
        rightset = src.rightset;
    }
    
    /** Create a new box from the same DOM node in the same context */
    public BlockBox copyBlockBox()
    {
        BlockBox ret = new BlockBox(el, g, ctx);
        ret.copyValues(this);
        return ret;
    }
    
    //========================================================================
    
    public String toString()
    {
        return "<" + el.getTagName() + " id=\"" + el.getAttribute("id") + 
               "\" class=\""  + el.getAttribute("class") + "\">";
    }
    
    public void setStyle(CSSStyleDeclaration s)
    {
    	super.setStyle(s);
    	loadBlockStyle();
    }
    
    public boolean containsBlocks()
    {
        return contblock;
    }
    
    public void setFloats(FloatList left, FloatList right, int xl, int xr, int y)
    {
        fleft = left;
        fright = right;
        floatXl = xl;
        floatXr = xr;
        floatY = y;
    }
    
    public void setOwnerFloatList(FloatList list)
    {
    	fown = list;
    }

    public FloatList getOwnerFloatList()
    {
    	return fown;
    }
    
    public short getFloating()
    {
        return floating;
    }

    public short getClearing()
    {
        return clearing;
    }
    
    /** Returns true if the box is in the normal text flow (not absolutely
     * positioned nor floating */
    @Override
    public boolean isInFlow()
    {
        return (displayed && floating == FLOAT_NONE && position != POS_ABSOLUTE && position != POS_FIXED);
    }
    
	public boolean containsFlow()
	{
		return anyinflow;
	}

	/** Returns true if the element displays at least something */
    public boolean affectsDisplay()
    {
        boolean ret = containsFlow();
        //non-zero top or left border
        if (border.top > 0 || border.bottom > 0)
            ret = true;
        //the same with padding
        if (padding.top > 0 || padding.bottom > 0)
            ret = true;
        
        return ret;
    }
	
    @Override
    public boolean canSplitInside()
    {
        return false;
    }

    @Override
    public boolean canSplitBefore()
    {
        return true;
    }

    @Override
    public boolean canSplitAfter()
    {
        return true;
    }

    /**
     * If none of the width parts is "auto", returns the total preferred width. Otherwise,
     * -1 is returned.
     * @return the preferred width
     */
    public int getPreferredWidth()
    {
        return preferredWidth;
    }
    
    /**
     * Sets the width of the content while considering the min- and max- width.
     * @param width the width the set if possible
     */
    public void setContentWidth(int width)
    {
        int w = width;
        if (max_size.width != -1 && w > max_size.width)
            w = max_size.width;
        if (min_size.width != -1 && w < min_size.width)
            w = min_size.width;
        content.width = w;
    }
    
    /**
     * Sets the height of the content while considering the min- and max- height.
     * @param height the height the set if possible
     */
    public void setContentHeight(int height)
    {
        int h = height;
        if (max_size.height != -1 && h > max_size.height)
            h = max_size.height;
        if (min_size.height != -1 && h < min_size.height)
            h = min_size.height;
        content.height = h;
    }
    
   //========================================================================
    
    /** 
     * Align the subboxes in a line according to the selected alignment settings
     * @param linestrt Index of the first subbox on the line
     * @param lineend Index of the last subbox on the line (excluding)
     * @param curw Content width when aligned to the left
     * @param maxw Total line width
     */
    private void alignLine(int linestrt, int lineend, int curw, int maxw)
    {
    	//TODO: align: justify
        int dif = maxw - curw; //difference between maximal and current width
        if (align != ALIGN_LEFT && dif > 0)
        {
            for (int i = linestrt; i < lineend; i++) //all boxes on this line
            {
                if (align == ALIGN_RIGHT)
                    getSubBox(i).moveRight(dif);
                else if (align == ALIGN_CENTER)
                    getSubBox(i).moveRight(dif/2);
            }
        }
    }

    /** Compute the width and height of this element. Layout the sub-elements.
     * @param availw Maximal width available to the child elements
     * @param force Use the area even if the used width is greater than maxwidth
     * @param linestart Indicates whether the element is placed at the line start
     * @return <code>true</code> if the box has been succesfully placed
     */
    public boolean doLayout(int availw, boolean force, boolean linestart)
    {
        //Skip if not displayed
        if (!displayed)
        {
            content.setSize(0, 0);
            bounds.setSize(0, 0);
            return true;
        }

        //remove previously splitted children from possible previous layout
        clearSplitted();

        /* Always try to use the full width. If the box is not in flow, its width
         * is updated after the layout */
        setAvailableWidth(totalWidth());
        
        if (!contblock)  //block elements containing inline elements only
            layoutInline();
        else //block elements containing block elements
            layoutBlocks();
        //allways fits as well possible
        return true;
    }

    /**
     * Lay out inline boxes inside of this block
     */
    private void layoutInline()
    {
        int x1 = fleft.getWidth(floatY) - floatXl;
        int x2 = fright.getWidth(floatY) - floatXr;
        if (x1 < 0) x1 = 0;
        if (x2 < 0) x2 = 0;
        int wlimit = getMaxContentWidth();
        int x = x1; //current x
        int y = 0; //current y
        int maxw = 0; //width of the longest line found
        int maxh = 0; //maximal height on the line
        int lnstr = 0; //the index of the first subbox on current line
        int lastbreak = 0; //last possible position of a line break

        Vector<ContentLine> lines = new Vector<ContentLine>();
        ContentLine curline = new ContentLine(0);
        lines.add(curline);
        
        //TODO: If it only has inline-level children, the height is the distance between the top of the topmost line box and the bottom of the bottommost line box.
        // http://www.w3.org/TR/CSS21/visudet.html#normal-block
        
        for (int i = 0; i < getSubBoxNumber(); i++)
        {
            Box subbox = getSubBox(i);
            if (subbox.canSplitBefore())
            	lastbreak = i;
            
            //if we find a block here, it must be an out-of-flow box
            //make the positioning and continue
            if (subbox.isBlock())
            {
                BlockBox sb = (BlockBox) subbox;
                BlockLayoutStatus stat = new BlockLayoutStatus();
                stat.y = y + maxh; //floating boxes should start below this line
                if (sb.getFloating() == FLOAT_LEFT || sb.getFloating() == FLOAT_RIGHT) //floating boxes
                    layoutBlockFloating(sb, wlimit, stat);
                else //absolute or fixed positioning
                    layoutBlockPositioned(sb, wlimit, stat);
                
                //in case the block was floating, we need to update the bounds
                x1 = fleft.getWidth(y + floatY) - floatXl;
                x2 = fright.getWidth(y + floatY) - floatXr;
                if (x1 < 0) x1 = 0;
                if (x2 < 0) x2 = 0;
                x = x1;
                //continue with next subboxes
                continue;
            }
            
            //process inline elements
            boolean split;
            do //repeat while the box is being split to sub-boxes
            {
                split = false;
                int space = wlimit - x1 - x2; //total space on the line
                boolean narrowed = (x1 > 0 || x2 > 0); //the space is narrowed by floats
                //force: we're at the leftmost position or the line cannot be broken
                // if there is no space on the line because of the floats, do not force
                boolean f = (x == x1 || lastbreak == lnstr) && (space >= INFLOW_SPACE_THRESHOLD || !narrowed);
                //do the layout
                boolean fit = subbox.doLayout(wlimit - x - x2, f, x == x1);
                if (fit) //positioning succeeded
                {
                    if (subbox.isInFlow())
                    {
                        subbox.setPosition(x,  y);
                        x += subbox.getWidth();
                    }
                }
                
                //update the maximal line height
                if (subbox instanceof InlineBox)
                {
                    InlineBox isubbox = (InlineBox) subbox;
                    if (isubbox.getMaxLineHeight() > maxh) 
                        maxh = isubbox.getMaxLineHeight();
                }
                else
                {
                    if (subbox.getHeight() > maxh)
                        maxh = subbox.getHeight();
                }
                
                //check line overflows
                if (!fit && space < INFLOW_SPACE_THRESHOLD && narrowed) //failed because of no space caused by floats
                {
                    //go to the new line
                    if (x > maxw) maxw = x;
                    y += getLineHeight();
                    maxh = 0;
                    x1 = fleft.getWidth(y + floatY) - floatXl;
                    x2 = fright.getWidth(y + floatY) - floatXr;
                    if (x1 < 0) x1 = 0;
                    if (x2 < 0) x2 = 0;
                    x = x1;
                    //force repeating the same once again
                    split = true;
                }
                else if (((!fit || x >= wlimit - x2) && lastbreak > lnstr) //line overflow and the line can be broken
                           || (fit && subbox.getRest() != null)) //or something fit but something has left
                {
                    //the width for text alignment
                    curline.setWidth(x);
                    curline.setLimits(x2);
                    //go to the new line
                    if (x > maxw) maxw = x;
                    y += getLineHeight();
                    maxh = 0;
                    x1 = fleft.getWidth(y + floatY) - floatXl;
                    x2 = fright.getWidth(y + floatY) - floatXr;
                    if (x1 < 0) x1 = 0;
                    if (x2 < 0) x2 = 0;
                    x = x1;
                    //create the new line record for alignment
                    lnstr = i+1;
                    curline.setEnd(lnstr); //finish the old line
                    curline = new ContentLine(lnstr);
                    lines.add(curline);
                    
                    if (!fit)
                    	split = true; //force repeating the same once again
                    else 
                    	if (subbox.getRest() != null) //not everything placed
                    		insertSubBox(i+1, subbox.getRest());
                }
            } while (split);
            
            if (subbox.canSplitAfter())
            	lastbreak = i+1;
       }
        
        //block width
        if (x > maxw) maxw = x;
        lastPreferredWidth = maxw;
        //Update width if it is not determined (out of the flow boxes with no width set)
        //TODO: here, overflow: should be considered. Using owerflow: visible
        if (!hasFixedWidth() && !isInFlow())
        {
        	int min = getMinimalContentWidthLimit();
        	int pref = maxw;
        	if (pref < min) pref = min;
            setContentWidth(pref); //shrintk-to-fit
            updateChildSizes();
        }
        if (!hasFixedHeight())
        {
                y = y + maxh; //possible unfinished line
                if (overflow != OVERFLOW_VISIBLE || floating != FLOAT_NONE || display == ElementBox.DISPLAY_INLINE_BLOCK)
                {
                    //enclose all floating boxes we own
                    // http://www.w3.org/TR/CSS21/visudet.html#root-height
                    int mfy = getFloatHeight() - floatY;
                    if (mfy > y) y = mfy;
                }
                //the total height is the last Y coordinate
                setContentHeight(y);
        }
        setSize(totalWidth(), totalHeight());
        
        //align the lines according to the real box width
        curline.setWidth(x); //last line width
        curline.setEnd(getSubBoxNumber());
        for (Iterator<ContentLine> it = lines.iterator(); it.hasNext();)
        {
            ContentLine line = it.next();
            alignLine(line.getStart(), line.getEnd(), line.getWidth(), content.width - line.getLimits());
        }
    }

    /**
     * Lay out nested block boxes in this box
     */
    private void layoutBlocks()
    {
        int wlimit = getMaxContentWidth();
        BlockLayoutStatus stat = new BlockLayoutStatus();

        for (int i = 0; i < getSubBoxNumber(); i++)
        {
            int nexty = stat.y; //y coordinate after positioning the subbox 
            BlockBox subbox = (BlockBox) getSubBox(i);
            
            if (subbox.isDisplayed())
            {
                //clear set - try to find the first possible Y value
                if (subbox.getClearing() != CLEAR_NONE)
                {
                    int ny = stat.y;
                    if (subbox.getClearing() == CLEAR_LEFT)
                        ny = fleft.getMaxY() - floatY;
                    else if (subbox.getClearing() == CLEAR_RIGHT)
                        ny = fright.getMaxY() - floatY;
                    else if (subbox.getClearing() == CLEAR_BOTH)
                        ny = Math.max(fleft.getMaxY(), fright.getMaxY()) - floatY;
                    if (stat.y < ny) stat.y = ny;
                }
                
                if (subbox.isInFlow()) //normal flow
                {
                    layoutBlockInFlow(subbox, wlimit, stat);
                    if (subbox.getRest() != null) //not everything placed -- insert the rest to the queue
                        insertSubBox(i+1, subbox.getRest());
                    nexty = stat.y; //the flow influences current y
                }
                else if (subbox.getFloating() == FLOAT_LEFT || subbox.getFloating() == FLOAT_RIGHT) //floating boxes
                {
                    layoutBlockFloating(subbox, wlimit, stat);
                }
                else //absolute or fixed positioning
                {
                    layoutBlockPositioned(subbox, wlimit, stat);
                }
                //accept the resulting Y coordinate
                stat.y = nexty;
            }
        }

        //Update width if not set (out of the flow boxes with no width set)
        if (!hasFixedWidth())
        {
        	int min = getMinimalContentWidthLimit();
        	int pref = stat.prefw;
        	if (pref < min) pref = min;
            setContentWidth(pref); //shrintk-to-fit
            updateChildSizes();
        }
        //update the height when not set or set to "auto"
        if (!hasFixedHeight())
        {
            if (overflow != OVERFLOW_VISIBLE || floating != FLOAT_NONE || display == ElementBox.DISPLAY_INLINE_BLOCK)
            {
                //enclose all floating boxes we own
                // http://www.w3.org/TR/CSS21/visudet.html#root-height
                int mfy = getFloatHeight() - floatY;
                if (mfy > stat.y) stat.y = mfy;
            }
            //the total height is the last Y coordinate
            setContentHeight(stat.y);
        }
        setSize(totalWidth(), totalHeight());
    }

    private void layoutBlockInFlow(BlockBox subbox, int wlimit, BlockLayoutStatus stat)
    {
        //new floating box limits
        int newfloatXl = floatXl + subbox.emargin.left
                            + subbox.border.left + subbox.padding.left;
        int newfloatXr = floatXr + subbox.emargin.right
                            + subbox.border.right + subbox.padding.right;
        int newfloatY = floatY + subbox.emargin.top
                            + subbox.border.top + subbox.padding.top;
        
        //position the box
        subbox.setFloats(fleft, fright, newfloatXl, newfloatXr, stat.y + newfloatY);
        subbox.setPosition(0,  stat.y);
        subbox.doLayout(wlimit, true, true);
        stat.y += subbox.getHeight();
        //maximal width
        if (subbox.getWidth() > stat.maxw)
            stat.maxw = subbox.getWidth();
        //preferred width
        int pref = subbox.getPreferredWidth();
        if (pref == -1) pref = subbox.getWidth(); //nothing preferred, we use the maximal width
        if (pref > stat.prefw)
            stat.prefw = pref;
    }
    
    private void layoutBlockFloating(BlockBox subbox, int wlimit, BlockLayoutStatus stat)
    {
        subbox.setFloats(new FloatList(subbox), new FloatList(subbox), 0, 0, 0);
        subbox.doLayout(wlimit, true, true);
        FloatList f = (subbox.getFloating() == FLOAT_LEFT) ? fleft : fright;
        FloatList of = (subbox.getFloating() == FLOAT_LEFT) ? fright : fleft;
        int floatX = (subbox.getFloating() == FLOAT_LEFT) ? floatXl : floatXr;
        int fy = stat.y + floatY;  //float Y position
        int fx = f.getWidth(fy);   //total width of floats at this side
        int ofx = of.getWidth(fy); //total width of floats at the opposite side
        if (fx < floatX) fx = floatX;
        //moving the floating box down until it fits
        while (fx > floatX //if we're not already at the left/right border
               && (fx + subbox.getWidth() > wlimit-ofx + floatX)) //the subbox doesn't fit in this Y coordinate
        {
            fy = f.getNextY(fy);
            fx = f.getWidth(fy);
            if (fx < floatX) fx = floatX;
        }
        subbox.setPosition(fx, fy);
        f.add(subbox);
        //a floating box must enclose all the floats inside
        int floatw = maxFloatWidth(fy, fy + subbox.getHeight());
        //maximal width
        if (floatw > stat.maxw) stat.maxw = floatw;
        if (stat.maxw > wlimit) stat.maxw = wlimit;
        //FIXME: is this correct? this shouldn't be based on maxFloadWidth()
        if (floatw > stat.prefw) stat.prefw = floatw;
        if (stat.prefw > wlimit) stat.prefw = wlimit;
    }
    
    private void layoutBlockPositioned(BlockBox subbox, int wlimit, BlockLayoutStatus stat)
    {
        //layout the contents
        subbox.setFloats(new FloatList(subbox), new FloatList(subbox), 0, 0, 0);
        subbox.doLayout(wlimit, true, true);
    }
    
    /** Calculate absolute positions of all the subboxes.
     * @param parent Parent element box.
     */
    public void absolutePositions(ElementBox parent)
    {
        if (displayed)
        {
            //my top left corner
            int x = cblock.getContentX() + bounds.x;
            int y = cblock.getContentY() + bounds.y;

            loadPosition();
            if (floating == FLOAT_NONE)
            {
                if (position == POS_RELATIVE)
                {
                    x += leftset ? coords.left : (-coords.right);
                    y += topset ? coords.top : (-coords.bottom);
                }
                else if (position == POS_ABSOLUTE || position == POS_FIXED)
                {
                	if (leftset)
                        x = cblock.getContentX() + coords.left;
                    else if (rightset)
                		x = cblock.getContentX() + cblock.getContentWidth() - bounds.width - coords.right - 2;
                    else
                        x = cblock.getContentX();
                	
                	if (topset)
                        y = cblock.getContentY() + coords.top;
                    else if (bottomset)
                		y = cblock.getContentY() + cblock.getContentHeight() - bounds.height - coords.bottom - 2;
                    else
                        y = cblock.getContentY();
                }
            }
            else if (floating == FLOAT_LEFT)
            {
            	BlockBox listowner = fown.getOwner();
                x = listowner.getContentX() + bounds.x;
                y = listowner.getContentY() + bounds.y;
            }
            else if (floating == FLOAT_RIGHT)
            {
            	BlockBox listowner = fown.getOwner();
                x = listowner.getContentX() + listowner.getContentWidth() - bounds.width - bounds.x - 2;
                y = listowner.getContentY() + bounds.y;
            }

            //set the absolute coordinates
            bounds.x = x;
            bounds.y = y;
            
            //repeat for all valid subboxes
            for (int i = startChild; i < endChild; i++)
                getSubBox(i).absolutePositions(this);
        }
    }

    public int getMaxContentWidth()
    {
        int ret = maxwidth - emargin.left - border.left - padding.left 
                  - padding.right - border.right - emargin.right;
        if (max_size.width != -1 && ret > max_size.width)
            ret = max_size.width;
        return ret;
    }
    
    public int getMinimalWidth()
    {
        int ret = 0;
        //if the width is set or known implicitely, return the width
        if (hasFixedWidth())
            ret = content.width;
        //return the maximum of the nested minimal widths
        else
            ret = getMinimalContentWidth();
        //increase by margin, padding, border
        ret += margin.left + padding.left + border.left +
               margin.right + padding.right + border.right;
        return ret;
    }

    protected int getMinimalContentWidth()
    {
        int ret = 0;
        for (int i = startChild; i < endChild; i++)
        {
            int w = getSubBox(i).getMinimalWidth();
            if (w > ret) ret = w;
        }
        return ret;
    }
    
    public int getMaximalWidth()
    {
        int ret;
        //if the width is set or known implicitely, return the width
        if (hasFixedWidth())
            ret = content.width;
        else
            ret = getMaximalContentWidth();
        //increase by margin, padding, border
        ret += margin.left + padding.left + border.left +
               margin.right + padding.right + border.right;
        return ret;
    }

    protected int getMaximalContentWidth()
    {
        int sum = 0;
        int max = 0;
        //the inline elements inside are summed up, the maximum of block ones is taken 
        for (int i = startChild; i < endChild; i++)
        {
            Box subbox = getSubBox(i);
            if (!subbox.isInFlow())
            {
                if (subbox.getMaximalWidth() > max) max = subbox.getMaximalWidth();
            }
            else if (!subbox.isBlock())
            {
                sum += subbox.getMaximalWidth();
            }
            else
            {
                if (subbox.getMaximalWidth() > max) max = subbox.getMaximalWidth();
                if (sum > max) max = sum;
                sum = 0; //end of line forced by the block
            }
        }
        return Math.max(sum, max);
    }
    
    /**
     * Determines the minimal width as it is limited by the ancestors.
     * @return the minimal width
     */ 
    public int getMinimalContentWidthLimit()
    {
    	int ret;
    	int dif = margin.left + padding.left + border.left +
               	  margin.right + padding.right + border.right;
    	if (wset)
    		ret = content.width;
    	else if (min_size.width != -1)
    		ret = min_size.width;
    	else if (isInFlow())
    		ret = cblock.getMinimalContentWidthLimit() - dif;
    	else
    		ret = 0;
    		
    	return ret;
    }
    
	@Override
	public boolean hasFixedWidth()
	{
		return wset || (isInFlow() && cblock.hasFixedWidth());
	}

	@Override
	public boolean hasFixedHeight()
	{
		return hset; //only true if the height is set explicitly
	}

    /**
     * Computes the maximal height of the floating boxes inside of this box and
     * all its children.
     * @return the total height of the floating boxes
     */
    public int getFloatHeight()
    {
        if (fleft != null && fright != null)
        {
            int mfy = Math.max(fleft.getMaxYForOwner(this), fright.getMaxYForOwner(this));
            if (this.containsBlocks())
            {
                for (int i = 0; i < getSubBoxNumber(); i++)
                {
                    Box subbox = getSubBox(i);
                    if (subbox instanceof BlockBox)
                    {
                        int cmfy = ((BlockBox) subbox).getFloatHeight();
                        if (cmfy > mfy) mfy = cmfy;
                    }
                }
            }
            return mfy;
        }
        else
            return 0;
    }
    
	@Override
    public void draw(Graphics g, int turn, int mode)
    {
        ctx.updateGraphics(g);
        if (displayed && isVisible())
        {
            int nestTurn = turn;
            switch (turn)
            {
                case DRAW_ALL: 
                    if (mode == DRAW_BOTH || mode == DRAW_BG) drawBackground(g);
                    nestTurn = DRAW_ALL;
                    break;
                case DRAW_NONFLOAT:
                    if (floating == FLOAT_NONE)
                    {
                        if (mode == DRAW_BOTH || mode == DRAW_BG) drawBackground(g);
                        nestTurn = DRAW_NONFLOAT;
                    }
                    break;
                case DRAW_FLOAT:
                    if (floating != FLOAT_NONE)
                    {
                        if (mode == DRAW_BOTH || mode == DRAW_BG) drawBackground(g);
                        nestTurn = DRAW_ALL;
                    }
                    break;
            }
            
            if (getElement().getAttribute("id").startsWith("d"))
                drawExtent(g);
            
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                for (int i = startChild; i < endChild; i++)
                    getSubBox(i).draw(g, nestTurn, mode);
            }
        }
    }
    
    //=======================================================================
    
    
    private void loadBlockStyle()
    {
        String fp = getStyleProperty("float");
        if (fp.equals("left")) floating = FLOAT_LEFT;
        else if (fp.equals("right")) floating = FLOAT_RIGHT;

        String cp = getStyleProperty("clear");
        if (cp.equals("left")) clearing = CLEAR_LEFT;
        else if (cp.equals("right")) clearing = CLEAR_RIGHT;
        else if (cp.equals("both")) clearing = CLEAR_BOTH;
        
        String pp = getStyleProperty("position");
        if (pp.equals("relative")) position = POS_RELATIVE;
        else if (pp.equals("absolute")) position = POS_ABSOLUTE;
        else if (pp.equals("fixed")) position = POS_FIXED;
        
        String op = getStyleProperty("overflow");
        if (op.equals("hidden")) overflow = OVERFLOW_HIDDEN;
        else if (op.equals("scroll")) overflow = OVERFLOW_SCROLL;
        else if (op.equals("auto")) overflow = OVERFLOW_AUTO;

        String ap = getStyleProperty("text-align");
        if (ap.equals("justify")) align = ALIGN_JUSTIFY;
        else if (ap.equals("center")) align = ALIGN_CENTER;
        else if (ap.equals("right")) align = ALIGN_RIGHT;
    }
    
    /**
     * Loads the top, left, bottom and right coordinates from the style
     */
    protected void loadPosition()
    {
        CSSDecoder dec = new CSSDecoder(ctx);

        int contw = cblock.getContentWidth();
        int conth = cblock.getContentHeight();
        
        coords = new LengthSet();
        coords.top = dec.getLength(getStyleProperty("top"), 0, 0, conth);
        coords.right = dec.getLength(getStyleProperty("right"), 0, 0, contw);
        coords.bottom = dec.getLength(getStyleProperty("bottom"), 0, 0, conth);
        coords.left = dec.getLength(getStyleProperty("left"), 0, 0, contw);
        topset = !(getStyleProperty("top").equals("") || getStyleProperty("top").equals("auto"));
        leftset = !(getStyleProperty("left").equals("") || getStyleProperty("left").equals("auto"));
        bottomset = !(getStyleProperty("bottom").equals("") || getStyleProperty("bottom").equals("auto"));
        rightset = !(getStyleProperty("right").equals("") || getStyleProperty("right").equals("auto"));
    }
    
    /** Compute the total width of a block element according to the min-, max-,
     * width properties */
    protected int blockWidth()
    {
    	return content.width;
    }

    /** Compute the height of a block element according to the min-, max-,
     * height properties */
    protected int blockHeight()
    {
    	return content.height;
    }

    /** Returns maximal width of floats in a specified Y interval */
    protected int maxFloatWidth(int y1, int y2)
    {
        int ret = 0;
        for (int y = y1; y <= y2; y++)
        {
            int w = fleft.getWidth(y) + fright.getWidth(y);
            if (w > ret)
                ret = w;
        }
        return ret;
    }

    protected void loadSizes()
    {
        loadSizes(false);
    }
    
    public void updateSizes()
    {
    	loadSizes(true);
    }
    
    /**
     * Load the sizes from CSS properties.
     * @param update Update mode after the size of the containing box has been updated.
     * The content size is not reset, the margins are recomputed.
     */  
    protected void loadSizes(boolean update)
    {
        CSSDecoder dec = new CSSDecoder(ctx);
        
        //containing box sizes
        if (cblock == null)
            { System.err.println(toString() + " has no cblock"); return; }
        int contw = cblock.getContentWidth();
        int conth = cblock.getContentHeight();
        
        //Borders
        if (!update) //borders needn't be updated
        {
            String medium = "3px";
            border = new LengthSet();
            if (borderVisible("top"))
            		border.top = dec.getLength(getStyleProperty("border-top-width"), medium, "0", 0);
            else
            		border.top = 0;
            if (borderVisible("right"))
    	    		border.right = dec.getLength(getStyleProperty("border-right-width"), medium, "0", 0);
    	    else
    	    		border.right = 0;
    	    if (borderVisible("bottom"))
    	    		border.bottom = dec.getLength(getStyleProperty("border-bottom-width"), medium, "0", 0);
    	    else
    	    		border.bottom = 0;
    	    if (borderVisible("left"))
    	    		border.left = dec.getLength(getStyleProperty("border-left-width"), medium, "0", 0);
    	    else
    	    		border.left = 0;
        }
        
        //Padding
        padding = new LengthSet();
        padding.top = dec.getLength(getStyleProperty("padding-top"), 0, 0, contw);
        padding.right = dec.getLength(getStyleProperty("padding-right"), 0, 0, contw);
        padding.bottom = dec.getLength(getStyleProperty("padding-bottom"), 0, 0, contw);
        padding.left = dec.getLength(getStyleProperty("padding-left"), 0, 0, contw);
        
        //Content and margins
        if (!update)
        {
        	content = new Dimension(0, 0);
        	margin = new LengthSet();
        }
            
        //Minimal and maximal width
        min_size = new Dimension(dec.getLength(getStyleProperty("min-width"), -1, -1, contw),
                                 dec.getLength(getStyleProperty("min-height"), -1, -1, conth));
        max_size = new Dimension(dec.getLength(getStyleProperty("max-width"), -1, -1, contw),
                                 dec.getLength(getStyleProperty("max-height"), -1, -1, conth));
        if (max_size.width != -1 && max_size.width < min_size.width)
            max_size.width = min_size.width;
        if (max_size.height != -1 && max_size.height < min_size.height)
            max_size.height = min_size.height; 
        
        //Calculate widths and margins
        String width = getStyleProperty("width");
        computeWidths(width, true, contw, update);
        if (max_size.width != -1 && content.width > max_size.width)
        {
            width = getStyleProperty("max-width");
            computeWidths(width, false, contw, update);
        }
        if (min_size.width != -1 && content.width < min_size.width)
        {
            width = getStyleProperty("min-width");
            computeWidths(width, false, contw, update);
        }
        
        //Calculate heights and margins
        // http://www.w3.org/TR/CSS21/visudet.html#Computing_heights_and_margins
        String height = getStyleProperty("height");
        computeHeights(height, true, contw, update);
        if (max_size.height != -1 && content.height > max_size.height)
        {
            height = getStyleProperty("max-height");
            computeHeights(height, false, contw, update);
        }
        if (min_size.height != -1 && content.height < min_size.height)
        {
            height = getStyleProperty("min-height");
            computeHeights(height, false, contw, update);
        }
        
        if (update)
        	emargin = new LengthSet(emargin.top,
        							margin.right,
        							emargin.bottom,
        							margin.left);
        else
        	emargin = new LengthSet(margin);
    }
    
    /** 
     * Calculates widths and margins according to
     *  http://www.w3.org/TR/CSS21/visudet.html#Computing_widths_and_margins .
     * @param width the specified width
     * @param exact true if this is the exact width, false when it's a max/min width
     * @param contw containing block width
     * @param wknown <code>true</code>, if the containing block width is known
     * @param update <code>true</code>, if we're just updating the size to a new containing block size
     */
    protected void computeWidths(String width, boolean exact, int contw, boolean update)
    {
        CSSDecoder dec = new CSSDecoder(ctx);
        
        if (width.equals("")) width = "auto";
        String mleft = getStyleProperty("margin-left");
        String mright = getStyleProperty("margin-right");
        preferredWidth = -1;
        
        if (width.equals("auto"))
        {
        	if (exact) wset = false;
            margin.left = dec.getLength(mleft, 0, 0, contw);
            margin.right = dec.getLength(mright, 0, 0, contw);
            /* For the first time, we always try to use the maximal width even for the
             * boxes out of the flow. When updating, only the in-flow boxes are adjusted. */
            if (!update || isInFlow())
            {
                content.width = contw - margin.left - border.left - padding.left
                                  - padding.right - border.right - margin.right;
                if (content.width < 0) content.width = 0;
            }
            if (isInFlow())
                preferredWidth = -1; //we don't prefer anything (auto width)
            else
                preferredWidth = margin.left + border.left + padding.left + padding.right
                + border.right + margin.right; 
        }
        else
        {
        	if (exact) wset = true;
          	content.width = dec.getLength(width, 0, 0, contw);
            margin.left = dec.getLength(mleft, 0, 0, contw);
            margin.right = dec.getLength(mright, 0, 0, contw);

            // TODO: pro absolutni pozicovani by to melo byt jinak
            if (isInFlow()) 
            {
                if (mleft.equals("auto") && mright.equals("auto"))
                {
                    preferredWidth = border.left + padding.left + content.width +
                                     padding.right + border.right;
                    int rest = contw - content.width - border.left - padding.left
                                     - padding.right - border.right;
                    if (rest < 0) rest = 0;
                    margin.left = (rest + 1) / 2;
                    margin.right = rest / 2;
                }
                else if (mleft.equals("auto"))
                {
                    preferredWidth = border.left + padding.left + content.width +
                                     padding.right + border.right + margin.right;
                    margin.left = contw - content.width - border.left - padding.left
                                        - padding.right - border.right - margin.right;
                    if (margin.left < 0) margin.left = 0; //"treated as zero"
                }
                else if (mright.equals("auto"))
                {
                    preferredWidth = margin.left + border.left + padding.left + content.width +
                                     padding.right + border.right;
                    margin.right = contw - content.width - border.left - padding.left
                                    - padding.right - border.right - margin.left;
                    if (margin.right < 0) margin.right = 0; //"treated as zero"
                }
                else //everything specified, ignore right margin
                {
                    preferredWidth = margin.left + border.left + padding.left + content.width +
                                        padding.right + border.right + margin.right;
                    margin.right = contw - content.width - border.left - padding.left
                                    - padding.right - border.right - margin.left;
                    if (margin.right < 0) margin.right = 0; //"treated as zero"
                }
            }
        }
    }

    /** 
     * Calculates heights and margins according to
     *  http://www.w3.org/TR/CSS21/visudet.html#Computing_heights_and_margins
     * @param height the specified width
     * @param exact true if this is the exact height, false when it's a max/min height
     * @param contw containing block width
     * @param wknown <code>true</code>, if the containing block width is known
     * @param update <code>true</code>, if we're just updating the size to a new containing block size
     */
    protected void computeHeights(String height, boolean exact, int contw, boolean update)
    {
        CSSDecoder dec = new CSSDecoder(ctx);
        if (cblock != null && cblock.hset)
        {
            hset = (exact && !height.equals("auto") && !height.equals(""));
            if (!update)
                content.height = dec.getLength(height, 0, 0, cblock.getContentHeight());
        }
        else
        {
            hset = (exact && !height.equals("auto") && !height.equals("") && !height.endsWith("%"));
            if (!update)
                content.height = dec.getLength(height, 0, 0, 0);
        }
        margin.top = dec.getLength(getStyleProperty("margin-top"), 0, 0, contw); //contw is ok here!
        margin.bottom = dec.getLength(getStyleProperty("margin-bottom"), 0, 0, contw);
    }
    
    /**
     * Re-calculates the sizes of all the child block boxes recursively.
     */
    public void updateChildSizes()
    {
    	for (int i = 0; i < getSubBoxNumber(); i++)
    	{
    		Box child = getSubBox(i);
    		if (child instanceof BlockBox)
    		{
    			BlockBox block = (BlockBox) child;
    			int oldw = block.getContentWidth();
    			int oldh = block.getContentHeight();
    			block.updateSizes();
   				block.setSize(block.totalWidth(), block.totalHeight());
   				//if something has changed, update the children
    			if (block.getContentWidth() != oldw || block.getContentHeight() != oldh)
    				block.updateChildSizes();
    		}
    	}
    }
   
    /**
     * @return <code>true</code> if the box has a visible border around
     */
    protected boolean borderVisible(String dir)
    {
    		String style = getStyleProperty("border-"+dir+"-style");
    		return (!style.equals("") && !style.equals("none") && !style.equals("hidden")); 
    }
    
    /**
     * Remove the previously splitted child boxes
     */
    private void clearSplitted()
    {
        for (Iterator<Box> it = nested.iterator(); it.hasNext(); )
        {
            Box box = it.next();
            if (box.splitted)
            {
                it.remove();
                endChild--;
            }
        }
    }
    
}

/**
 * A class describing the status of the block box layout
 */
class BlockLayoutStatus
{
    /** current <em>y</em> coordinate relatively to the content box */
    public int y;
    
    /** maximal width of the boxes laid out */
    public int maxw;
    
    /** preferred width of the boxes laid out */
    public int prefw;
    
    /** Creates a new initialized layout status */
    public BlockLayoutStatus()
    {
        y = 0;
        maxw = 0;
        prefw = 0;
    }
}