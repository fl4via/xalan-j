package org.apache.xpath.axes;

import javax.xml.transform.TransformerException;

import org.apache.xpath.XPathContext;
import org.apache.xpath.compiler.Compiler;
import org.apache.xpath.patterns.NodeTest;
import org.apache.xpath.objects.XObject;

import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.dtm.DTMFilter;
import org.apache.xml.dtm.Axis;
import org.apache.xml.dtm.DTMAxisIterator;

/**
 * <meta name="usage" content="advanced"/>
 * This class implements a general iterator for
 * those LocationSteps with only one step, and perhaps a predicate.
 * @see org.apache.xpath.axes.WalkerFactory#newLocPathIterator
 */
public class OneStepIterator extends ChildTestIterator
{
  /** The traversal axis from where the nodes will be filtered. */
  protected int m_axis = -1;

  /** The DTM inner traversal class, that corresponds to the super axis. */
  protected DTMAxisIterator m_iterator;

  /**
   * Create a OneStepIterator object.
   *
   * @param compiler A reference to the Compiler that contains the op map.
   * @param opPos The position within the op map, which contains the
   * location path expression for this itterator.
   *
   * @throws javax.xml.transform.TransformerException
   */
  public OneStepIterator(Compiler compiler, int opPos, int analysis)
          throws javax.xml.transform.TransformerException
  {
    super(compiler, opPos, analysis);
    int firstStepPos = compiler.getFirstChildPos(opPos);
    
    m_axis = WalkerFactory.getAxisFromStep(compiler, firstStepPos);
    
  }
  
  /**
   * Initialize the context values for this expression
   * after it is cloned.
   *
   * @param execContext The XPath runtime context for this
   * transformation.
   */
  public void initContext(XPathContext execContext)
  {
    super.initContext(execContext);
    m_iterator = m_cdtm.getAxisIterator(m_axis);
    m_iterator.setStartNode(m_context);
  }
  
  /**
   * Get the next node via getFirstAttribute && getNextAttribute.
   */
  protected int getNextNode()
  {
    return m_lastFetched = m_iterator.next();
  }
  
  /**
   * Get a cloned AxesWalker.
   *
   * @return A new AxesWalker that can be used without mutating this one.
   *
   * @throws CloneNotSupportedException
   */
  public Object clone() throws CloneNotSupportedException
  {
    // Do not access the location path itterator during this operation!
    
    OneStepIterator clone = (OneStepIterator) super.clone();

    if(m_iterator != null)
    {
      clone.m_iterator = m_iterator.cloneIterator();
    }
    return clone;
  }


  /**
   * Tells if this is a reverse axes.  Overrides AxesWalker#isReverseAxes.
   *
   * @return true for this class.
   */
  public boolean isReverseAxes()
  {
    return m_iterator.isReverse();
  }

  /**
   * Get the current sub-context position.  In order to do the
   * reverse axes count, for the moment this re-searches the axes
   * up to the predicate.  An optimization on this is to cache
   * the nodes searched, but, for the moment, this case is probably
   * rare enough that the added complexity isn't worth it.
   *
   * @param predicateIndex The predicate index of the proximity position.
   *
   * @return The pridicate index, or -1.
   */
  protected int getProximityPosition(int predicateIndex)
  {
    if(!isReverseAxes())
      return super.getProximityPosition(predicateIndex);
      
    // A negative predicate index seems to occur with
    // (preceding-sibling::*|following-sibling::*)/ancestor::*[position()]/*[position()]
    // -sb
    if(predicateIndex < 0)
      return -1;
      
    if (m_proximityPositions[predicateIndex] <= 0)
    {
      XPathContext xctxt = getXPathContext();
      try
      {
        OneStepIterator clone = (OneStepIterator) this.clone();

        xctxt.pushCurrentNode(getRoot());
        clone.initContext(xctxt);

        clone.setPredicateCount(predicateIndex);

        // Count 'em all
        int count = 1;
        int next;

        while (DTM.NULL != (next = clone.nextNode()))
        {
          count++;
        }

        m_proximityPositions[predicateIndex] += count;
      }
      catch (CloneNotSupportedException cnse)
      {

        // can't happen
      }
      finally
      {
        xctxt.popCurrentNode();
      }
    }

    return m_proximityPositions[predicateIndex];
  }

  /**
   * Count backwards one proximity position.
   *
   * @param i The predicate index.
   */
  protected void countProximityPosition(int i)
  {
    if(!isReverseAxes())
      super.countProximityPosition(i);
    else if (i < m_proximityPositions.length)
      m_proximityPositions[i]--;
  }

  /**
   * Get the number of nodes in this node list.  The function is probably ill
   * named?
   *
   *
   * @param xctxt The XPath runtime context.
   *
   * @return the number of nodes in this node list.
   */
  public int getLastPos(XPathContext xctxt)
  {
    if(!isReverseAxes())
      return super.getLastPos(xctxt);

    int count = 0;

    try
    {
      OneStepIterator clone = (OneStepIterator) this.clone();

      xctxt.pushCurrentNode(getRoot());
      clone.initContext(xctxt);

      clone.setPredicateCount(this.getPredicateCount() - 1);

      // Count 'em all
      // count = 1;
      int next;

      while (DTM.NULL != (next = clone.nextNode()))
      {
        count++;
      }
    }
    catch (CloneNotSupportedException cnse)
    {

      // can't happen
    }
    finally
    {
      xctxt.popCurrentNode();
    }

    // System.out.println("getLastPos - pos: "+count);
    // System.out.println("pos (ReverseAxesWalker): "+count);
    return count;
  }
}
