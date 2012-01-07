
package net.sf.sageplugins.webserver;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import net.sf.sageplugins.sageutils.SageApi;
import net.sf.sageplugins.sageutils.Translate;
import net.sf.sageplugins.webserver.utils.PluginUtils;
import sagex.api.AiringAPI;
import sagex.api.Global;

/**
 * @author Owner
 *
 * 
 *
 */
public class Airing {
	static final public int ID_TYPE_MEDIAFILE=0;
	static final public int ID_TYPE_AIRING=1;

	
	Object sageAiring;
	int idType;
	int id;
	
	Airing(HttpServletRequest req) throws Exception {
		idType=-1;
		id=-1;
		sageAiring=null;
		
		String idStr=req.getParameter("MediaFileId");
		if ( idStr != null && ! idStr.equals(""))
			idType=Airing.ID_TYPE_MEDIAFILE;
		else {
			idStr=req.getParameter("AiringId");
			if ( idStr != null && ! idStr.equals(""))
				idType=Airing.ID_TYPE_AIRING;
		} 
		if ( idStr != null ){
			try {
				id=Integer.parseInt(idStr);
				if ( idType==ID_TYPE_MEDIAFILE){
					sageAiring=SageApi.Api("GetMediaFileForID",new Object[]{new Integer(id)});
				} else if ( idType==ID_TYPE_AIRING){
					sageAiring=SageApi.Api("GetAiringForID",new Object[]{new Integer(id)});
				} 
			} catch (NumberFormatException e) {
                throw new Exception("Invalid ID passed: "+Translate.encode(idStr));
			}
            if (sageAiring==null) {
                if ( idType==ID_TYPE_AIRING )
                    throw new Exception("AiringId not found: "+Translate.encode(idStr));
                else
                    throw new Exception("MediaFileId not found: "+Translate.encode(idStr));
            }
            if ( idType==ID_TYPE_AIRING){
                Object mf=SageApi.Api("GetMediaFileForAiring",new Object[] {sageAiring});
                if ( mf != null) {
                    sageAiring=mf;
                    id=SageApi.IntApi("GetMediaFileID",new Object[]{mf});
                    idType=ID_TYPE_MEDIAFILE;
                }
            }
        } else {
            String filename=req.getParameter("FileName");
            if ( filename != null && filename.length()>0 ) {
                // check filenames against media directories
                Object mf=SageApi.Api("GetMediaFileForFilePath",filename);
                if ( mf == null )
                    throw new Exception("FileName: "+Translate.encode(filename)+" not found");
                sageAiring=mf;
                id=SageApi.IntApi("GetMediaFileID",new Object[]{mf});
                idType=ID_TYPE_MEDIAFILE;
                
            } else {
                throw new Exception("AiringId/MediaFileId/FileName not specified");
            }
                
        }

	}
	
    Airing(String filename) throws Exception{
        Object mf=SageApi.Api("GetMediaFileForFilePath",filename);
        if ( mf == null )
           throw new Exception("FileName: "+Translate.encode(filename)+" not found");
        sageAiring=mf;
        id=SageApi.IntApi("GetMediaFileID",new Object[]{mf});
        idType=ID_TYPE_MEDIAFILE;
    }
	
	Airing(int idType, int id) throws Exception {
		this.idType=idType;
		this.id=id;
		if ( idType==ID_TYPE_MEDIAFILE){
			sageAiring=SageApi.Api("GetMediaFileForID",new Object[]{new Integer(id)});
		} else if ( idType==ID_TYPE_AIRING){
			sageAiring=SageApi.Api("GetAiringForID",new Object[]{new Integer(id)});
		} else {
			throw new NumberFormatException("invalid idType");
		}
		if (sageAiring==null) {
			throw new Exception("airing Id not found");
		}
		if ( idType==ID_TYPE_AIRING){
		    Object mf=SageApi.Api("GetMediaFileForAiring",new Object[] {sageAiring});
		    if ( mf != null) {
		        sageAiring=mf;
		        idType=ID_TYPE_MEDIAFILE;
		        id=SageApi.IntApi("GetMediaFileID",new Object[]{mf});
		    }
		}
	}
	public Airing(Object sageAiring) throws Exception {
        // pseudo-copy
        if ( sageAiring instanceof Airing)
            sageAiring=((Airing)sageAiring).sageAiring;
        
		this.sageAiring=sageAiring;
		if ( SageApi.booleanApi("IsMediaFileObject",new Object[]{sageAiring}))
			idType=ID_TYPE_MEDIAFILE;
		else if ( SageApi.booleanApi("IsAiringObject",new Object[]{sageAiring}))
			idType=ID_TYPE_AIRING;
		else 
			throw new NumberFormatException("invalid Object");
		
		if ( idType==ID_TYPE_MEDIAFILE)
			this.id=SageApi.IntApi("GetMediaFileID",new Object[]{sageAiring});
		else if ( idType==ID_TYPE_AIRING)
			this.id=SageApi.IntApi("GetAiringID",new Object[]{sageAiring});
		else
			throw new NumberFormatException("invalid idType");
		if ( idType==ID_TYPE_AIRING){
		    Object mf=SageApi.Api("GetMediaFileForAiring",new Object[] {sageAiring});
		    if ( mf != null) {
		        this.sageAiring=mf;
		        idType=ID_TYPE_MEDIAFILE;
		        id=SageApi.IntApi("GetMediaFileID",new Object[]{mf});
		    }
		}
	}
	
	public boolean getWatched() throws Exception {
		return SageApi.booleanApi("IsWatched",new Object[]{sageAiring});
	}
    public boolean getFirstRun() throws Exception {
        return SageApi.booleanApi("IsShowFirstRun",new Object[]{sageAiring});
    }
    public boolean getHDTV() throws Exception {
        try {
            return SageApi.booleanApi("IsAiringHDTV",new Object[]{sageAiring});
        } catch (InvocationTargetException e) {
            // HDTV not supported
            return false;
        }
    }
	public String getTitle()  throws Exception {
		String title=SageApi.StringApi("GetAiringTitle",new Object[]{sageAiring});
		if (title == null || title.equals(""))
			return "<Unknown airing>";
		return title;
	}
	public String getEpisode()  throws Exception {
		return SageApi.StringApi("GetShowEpisode",new Object[]{sageAiring});
	}
	public Date getStartDate()  throws Exception {
		return new Date(((Long)SageApi.Api("GetAiringStartTime",new Object[]{sageAiring})).longValue());
	}
	public long getStartMillis()  throws Exception {
		return ((Long)SageApi.Api("GetAiringStartTime",new Object[]{sageAiring})).longValue();
	}
	public Date getEndDate()  throws Exception {
		return new Date(((Long)SageApi.Api("GetAiringEndTime",new Object[]{sageAiring})).longValue());
	}
	public long getEndMillis()  throws Exception {
		return ((Long)SageApi.Api("GetAiringEndTime",new Object[]{sageAiring})).longValue();
	}
	public long getDuration()  throws Exception {
		return (((Long)SageApi.Api("GetAiringDuration",new Object[]{sageAiring})).longValue());
	}
	public String getChannelNum()  throws Exception {
		return SageApi.StringApi("GetChannelNumber",new Object[]{sageAiring});
	}
	public String getChannelName()  throws Exception {
		return SageApi.StringApi("GetChannelName",new Object[]{sageAiring});
	}
	public String getChannel()  throws Exception {
		return getChannelNum()+"-"+getChannelName();
	}
	public String getAiringShortDescr() throws Exception{
		return SageApi.StringApi("PrintAiringShort",new Object[]{this.sageAiring});
	}
	public void printAiringTableCell(HttpServletRequest req,
	        						 PrintWriter out,
	        						 boolean hascheckbox,
	        						 boolean usechannellogos,
                                     boolean showmarkers,
                                     boolean showratings,
                                     boolean showepisodeid,
                                     boolean showfilesize,
	        						 Object RecSchedList,
                                     Object allConflicts,
                                     Object unresolvedConflicts)  throws Exception {
		String tdclass="";
		if ( getWatched()) {
			tdclass="watched";
		}
		out.println("   <div class=\"epgcell\"><table width=\"100%\" class=\"epgcellborder "+getBorderClassName()+" "+getBgClassName(false)+"\"><tr>");
		//out.println("   <td class=\""+getBorderClassName()+"\">");
		//out.println("   <table width=\" 100%\" class=\""+getBgClassName(false)+"\"><tr>");
		if ( hascheckbox ){
			out.print("      <td class='checkbox'><input type='checkbox' name=\"");
			if ( idType==ID_TYPE_AIRING)
				out.print("AiringId");
			if ( idType==ID_TYPE_MEDIAFILE)
				out.print("MediaFileId");
			out.println("\" value=\""+Integer.toString(id)+"\"" +
                    " onchange='javascript:checkGroupChecked(this)'/></td>");
		}
		out.println("      <td class=\"titlecell "+tdclass+"\">");
		out.println("<div class=\""+tdclass+"\">");
        //note -- no spaces between <img and titletext to prevent word-wrapping
		String infoLink;
		if ( idType==ID_TYPE_AIRING)
            infoLink="<a href=\"DetailedInfo?AiringId="+Integer.toString(id)+"\">";
        else if ( idType==ID_TYPE_MEDIAFILE)
            infoLink="<a href=\"DetailedInfo?MediaFileId="+Integer.toString(id)+"\">";
        else 
            throw new Exception("Unknown Airing Type: "+idType);
        
		String title=getTitle().trim();
        if ( title.length()==0)
            title="\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0"; // 10 non-breaking spaces
		
		if ( idType==ID_TYPE_MEDIAFILE){
			if ( SageApi.booleanApi("IsMusicFile",new Object[]{sageAiring})){
				title="Album: "+title;
				String s=(String)SageApi.Api("GetPeopleInShow",sageAiring);
				if ( s != null && s.length()>0) {
					title=title+" by: "+s;
				}
			} else if ( SageApi.booleanApi("IsPictureFile",new Object[]{sageAiring})){
				title="Picture: "+title;
			}			
		}
        out.println(infoLink);
		out.println(Translate.encode(title));
		

        String seasonAndEpisode = null;
		String ep=getEpisode();
        if (  ep != null && ep.trim().length()==0)
            ep =null;
        // only look for episode IDs for Airings, or for TV files
		if ( showepisodeid && 
		        ( idType == ID_TYPE_AIRING 
		                || (idType == ID_TYPE_MEDIAFILE 
		                        && SageApi.booleanApi("IsTVFile",new Object[]{sageAiring} )
		                )
		        ) 
		){
            // from malore's menus
            //If(Size(GetShowExternalID(Airing))>=12,GetShowExternalID(Airing),"00000000000000000000")
            //If(Substring(DummyEpisodeNum, 8, 12) != "0000",  Substring(DummyEpisodeNum, 8, 12), "") 
            String epId=(String)SageApi.Api("GetShowExternalID",sageAiring);
            int epIdx = epId.length() - 4; // show the last 4 chars
            if ( epId != null
                 && epId.length()>=12 
                 && ! epId.substring(epIdx).matches("^0*$"))
                if (  ep != null )
                    ep=epId.substring(epIdx)+" - "+ep;
                else
                    ep=epId.substring(epIdx);
        }
		if ( ep != null && ! ep.equals("")){
			out.println("         <br/>"+Translate.encode(ep));
		}
		out.println("      </a></div></td>");
		
        if ( idType == ID_TYPE_AIRING || 
            ( idType == ID_TYPE_MEDIAFILE &&
              SageApi.booleanApi("IsTVFile",new Object[]{sageAiring} )
           )
        ){
            Integer seasonNumber = (Integer)SageApi.Api("GetShowSeasonNumber", new Object[]{sageAiring});
            Integer episodeNumber = (Integer)SageApi.Api("GetShowEpisodeNumber", new Object[]{sageAiring});
            if (((seasonNumber != null) && (seasonNumber > 0)) ||
                ((episodeNumber != null) &&(episodeNumber > 0))) {
                if ((seasonNumber > 0)) {
                    seasonAndEpisode = "Season " + seasonNumber;
                }
                if ((seasonNumber > 0) && (episodeNumber > 0)) {
                    seasonAndEpisode += "<br />";
                }
                if ((episodeNumber > 0)) {
                    seasonAndEpisode += "Episode " + episodeNumber;
                }
                out.println("      <td class=\"seasonepisodecell\"><div class=\""+tdclass+"\">"+infoLink+seasonAndEpisode+"</a></div></td>");
            }
        }
		
		if ( showfilesize ) {
            String sizeStr=null;
            if ( idType==ID_TYPE_MEDIAFILE){
                Long size=(Long)SageApi.Api("GetSize",sageAiring);
                if ( size != null) {
                    DecimalFormat fmt=new DecimalFormat("0.00");
                    if (size.longValue() > 100000000l)
                        sizeStr=fmt.format(size.doubleValue()/1000000000.0)+"GB";
                    else 
                        sizeStr=fmt.format(size.doubleValue()/1000000.0)+"MB";
                }
            }
            if ( sizeStr != null)
                out.println("      <td class=\"filesizecell\"><div class=\""+tdclass+"\">"+infoLink+sizeStr+"</a></div></td>");
            else
                out.println("      <td class=\"filesizecell\"></td>");
        }
        
        if ( showmarkers && 
             ( idType==ID_TYPE_AIRING 
               || SageApi.booleanApi("IsTVFile",new Object[]{sageAiring}))){
            out.println("      <td class=\"markercell\">"+infoLink);
            if ( idType==ID_TYPE_MEDIAFILE 
                 || ( RecSchedList!=null && 
                      1==SageApi.Size(SageApi.Api("DataIntersection",new Object[]{RecSchedList,this.sageAiring})))) {
                // mark recording type
                if ( SageApi.booleanApi("IsFavorite",new Object[]{sageAiring})
                     && ! SageApi.booleanApi("IsManualRecord",new Object[]{sageAiring})) {
                    Object favorite=SageApi.Api("GetFavoriteForAiring",sageAiring);
                    if ( SageApi.booleanApi("IsFirstRunsOnly",new Object[]{favorite}))
                        out.print  ("<img src=\"RecordFavFirst.gif\" alt=\"Favorite - First Runs Only\"/>");
                    else if (SageApi.booleanApi("IsReRunsOnly",new Object[]{favorite}))
                        out.print  ("<img src=\"RecordFavRerun.gif\" alt=\"Favorite - Reruns Only\"/>");
                    else
                        out.print  ("<img src=\"RecordFavAll.gif\" alt=\"Favorite - First Runs and Reruns\"/>");
                } else if (SageApi.booleanApi("IsManualRecord",new Object[]{sageAiring})){
                    out.print  ("<img src=\"RecordMR.gif\" alt=\"Manual Recording\"/>");                        
                } else {
                    out.print  ("<img src=\"Markerblank.gif\" alt=\"\"/>");
                }
            } else if ((allConflicts != null) && (unresolvedConflicts != null)){
                // conflict icon
                // (Manual?) conflicts at some point got their own airing id so the epg airing does not show up on the conflicts list
                if (SageApi.booleanApi("IsManualRecord", new Object[]{sageAiring}) &&
                    SageApi.Api("GetMediaFileForAiring", sageAiring) == null) {
                    out.print("<img src=\"conflicticon.gif\" class=\"UnresolvedConflictIndicator\" alt=\"Unresolved Conflict\" title=\"Unresolved Conflict\"/>");
                } else if ( 1==SageApi.Size(SageApi.Api("DataIntersection",new Object[]{allConflicts,sageAiring}))) {
                    if ( 1==SageApi.Size(SageApi.Api("DataIntersection",new Object[]{unresolvedConflicts,sageAiring})))  {
                        out.print("<img src=\"conflicticon.gif\" class=\"UnresolvedConflictIndicator\" alt=\"Unresolved Conflict\" title=\"Unresolved Conflict\"/>");
                    } else {
                        out.print("<img src=\"resolvedconflicticon.gif\" class=\"ResolvedConflictIndicator\" alt=\"Resolved Conflict\" title=\"Resolved Conflict\"/>");
                    }
                } else {
                    out.print  ("<img src=\"Markerblank.gif\" alt=\"\"/>");
                }
            } else {
                out.print  ("<img src=\"Markerblank.gif\" alt=\"\"/>");
            }
            if ( SageApi.booleanApi("IsShowFirstRun",new Object[]{sageAiring}) )
                out.print  ("<img src=\"MarkerFirstRun.gif\" alt=\"First Run\"/>");
            else
                out.print  ("<img src=\"Markerblank.gif\" alt=\"\"/>");
            if ( SageApi.booleanApi("IsWatched",new Object[]{sageAiring}) )
                out.print  ("<img src=\"MarkerWatched.gif\" alt=\"Watched\"/>");
            else
                out.print  ("<img src=\"Markerblank.gif\" alt=\"\"/>");
            if(!AiringAPI.IsNotManualOrFavorite(sageAiring) && AiringAPI.GetScheduleEndTime(sageAiring) >= System.currentTimeMillis() && PluginUtils.isServerPluginInstalled("sre", "4\\..+") && (!Global.IsClient() || PluginUtils.isClientPluginInstalled("sre", "4\\..+"))) {
            	com.google.code.sagetvaddons.sre.engine.DataStore ds = com.google.code.sagetvaddons.sre.engine.DataStore.getInstance();
            	com.google.code.sagetvaddons.sre.engine.MonitorStatus status = ds.getMonitorStatusByObj(sageAiring);
            	out.print(String.format("<img src=\"sre4/%s.png\" alt=\"%s\" />", status.toString(), com.google.code.sagetvaddons.sre.engine.MonitorStatus.getToolTip(status.toString())));
            }
            out.println("<br/>");
            // HDTV
            String extraInf=SageApi.StringApi("GetExtraAiringDetails",new Object[]{sageAiring});
            if ( extraInf!=null && extraInf.indexOf("HDTV")>=0)
                out.print  ("<img src=\"MarkerHDTV.gif\" alt=\"HDTV marker\"/>");
            else
                out.print  ("<img src=\"Markerblank.gif\" alt=\"\"/>");
           
            if ( idType==ID_TYPE_MEDIAFILE  && SageApi.booleanApi("IsLibraryFile",new Object[]{sageAiring}) )
                out.print  ("<img src=\"MarkerArchived.gif\" alt=\"Archived File\"/>");
            else
                out.print  ("<img src=\"Markerblank.gif\" alt=\"\"/>");
            if ( SageApi.booleanApi("IsDontLike",new Object[]{sageAiring}) )
                out.print  ("<img src=\"MarkerDontLike.gif\" alt=\"Dont Like\"/>");
            else
                out.print  ("<img src=\"Markerblank.gif\" alt=\"\"/>");
            out.println("      </a></td>");
        }                    

        // ratings
        if ( showratings && 
             ( idType==ID_TYPE_AIRING 
               || SageApi.booleanApi("IsTVFile",new Object[]{sageAiring}))){
            String rating = SageApi.StringApi("GetParentalRating",new Object[]{sageAiring});
            String rated = SageApi.StringApi("GetShowRated",new Object[]{sageAiring});
            out.println("      <td class=\"ratingcell\">");
            out.println("<div class=\""+tdclass+"\">"+infoLink);
            if ((rating != null) && (rating.trim().length() > 0)) {
                out.print  ("<img src=\"Rating_"+rating+".gif\" alt=\""+rating+"\"/>");
            }
            out.println("      </a></div>");
            out.println("      </td>");
            out.println("      <td class=\"ratedcell\">"+infoLink);
            out.println("<div class=\""+tdclass+"\">");
            if ((rated != null) && (rated.trim().length() > 0) && (!rated.trim().equals("None"))) {
                out.println("<img src=\"Rating_" + rated + ".gif\" alt=\"" + rated + "\"/>");
            }
            out.println("      </a></div>");
            out.println("      </td>");
        }

        // Date
        Date date=getStartDate();
        SimpleDateFormat df = new SimpleDateFormat("EEE, MMM d, yyyy");
        DateFormat dtf = DateFormat.getTimeInstance(DateFormat.SHORT);
        out.println("      <td class=\"datecell\"><div class=\""+tdclass+"\">"
                +infoLink+df.format(date)+"<br/>"+dtf.format(date));
        date=getEndDate();
        out.println("&nbsp;-&nbsp;"+dtf.format(date)+"\r\n"
                +"</a></div></td>");
        
        // show channel for TV shows, 
        // show thumbnail for mediafiles
        out.println("      <td class=\"channelcell\"><div class=\""+tdclass+"\">"+infoLink);
        if ( idType==ID_TYPE_AIRING 
                || SageApi.booleanApi("IsTVFile",new Object[]{sageAiring})) {
            out.println("        "+Translate.encode(getChannelNum())+" - ");
    		Object channel=SageApi.Api("GetChannel",sageAiring);
    		if ( usechannellogos && null != SageApi.Api("GetChannelLogo",channel) ) {
    		    String chID=SageApi.Api("GetStationID",new Object[]{channel}).toString();
    		    out.println("<img class=\"infochannellogo\" src=\"ChannelLogo?ChannelID="+chID+"&type=Med&index=1&fallback=true\" alt=\""+Translate.encode(getChannelName())+" logo\" title=\""+Translate.encode(getChannelName())+"\"/>");
    		} else {
    			out.println(Translate.encode(getChannelName()));
    		}
    		
        } else {
            // ! TV && mediafile
            // workaround for NegativeArraySizeException thrown by HasAlbumArt
            // it still prints an exception to the console, but not to the web page
            boolean isMusicFile = SageApi.booleanApi("IsMusicFile",new Object[]{sageAiring});
            Object album = null;
            boolean hasAlbumArt = false;

            if (isMusicFile) {
                album = SageApi.Api("GetAlbumForFile",new Object[]{sageAiring});
                
                if (album != null) {
                    try {
                        hasAlbumArt = SageApi.booleanApi("HasAlbumArt",new Object[]{album});
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }

            if (isMusicFile
                && null != album
                &&  hasAlbumArt){
                // album art
                out.print("<img class=\"infochannellogo\" src=\"MediaFileThumbnail?small=yes&");
                String s = (String)SageApi.Api("GetAlbumName",album); s=(s==null?"":s);
                out.print("&albumname="+URLEncoder.encode(s,req.getCharacterEncoding()));
                s = (String)SageApi.Api("GetAlbumArtist",album); s=(s==null?"":s);
                out.print("&artist="+URLEncoder.encode(s,req.getCharacterEncoding()));
                s = (String)SageApi.Api("GetAlbumGenre",album); s=(s==null?"":s);
                out.print("&genre="+URLEncoder.encode(s,req.getCharacterEncoding()));
                s = (String)SageApi.Api("GetAlbumYear",album); s=(s==null?"":s);
                out.print("&year="+URLEncoder.encode(s,req.getCharacterEncoding()));
                out.print("\" alt=\"\"/>");
            } else if ( SageApi.booleanApi("HasAnyThumbnail",new Object[]{sageAiring}) ) {
                // track art
                out.println("<img class=\"infochannellogo\" src=\"MediaFileThumbnail?MediaFileId="+id+"&small=yes\" alt=\"\"/>");
            }
        }
        out.println("      </a></div></td>");
        
		out.println("   </tr></table></div>");//</td></tr></table>");
	}
	public String getBorderClassName() throws Exception {
		if ( SageApi.booleanApi("IsFileCurrentlyRecording",new Object[]{sageAiring}))
			return "showrecording";
		if ( SageApi.booleanApi("IsManualRecord",new Object[]{sageAiring}))
			return "showmanrec";
		if ( SageApi.booleanApi("IsDontLike",new Object[]{sageAiring}))
			return "showdontlike";
		if ( SageApi.booleanApi("IsFavorite",new Object[]{sageAiring}))
			return "showfavorite";
		return "showother";
	}
	public String getBgClassName(boolean includeOld) throws Exception {
	    String BgClassName = ""; // the value to be returned
        String BgClassNamePrefix = ""; // store the prefix separately since it will be added to 'other', category, and subcategory

        // figure out the prefix
        if ( includeOld && this.getEndMillis() < System.currentTimeMillis())
	    {
            // shows that aired in the past
	        BgClassNamePrefix="oldshow_";
	    }
	    else
	    {
	        // shows that are currently airing or will be aired in the future
            BgClassNamePrefix="show_";
	    }
        // 'other' is the default style and appears first in the list
	    BgClassName=BgClassNamePrefix+"other";
	    // add category and subcategory if they have values
	    // remove all spaces and dashes.  spaces separate different styles in html class attributes
	    // and dashes are used to separate the category from the subcategory
	    String category=SageApi.StringApi("GetShowCategory", new Object[] {sageAiring});
	    if (category != null && category.trim().length() > 0)
	    {
	        category = category.replaceAll("[ -]", "").toLowerCase();  // remove blanks and dashes
	        BgClassName=BgClassName+" "+BgClassNamePrefix+category;
	    
            String subCategory=SageApi.StringApi("GetShowSubCategory", new Object[] {sageAiring});
            if (subCategory != null && subCategory.trim().length() > 0)
            {
                subCategory = subCategory.replaceAll("[ -]", "").toLowerCase();  // remove blanks and dashes
                BgClassName=BgClassName+" "+BgClassNamePrefix+category+"-"+subCategory;
            }
	    }
        return BgClassName;
	}
	public void printIdArg(PrintWriter out)  throws Exception {
		out.print(getIdArg());
	}
	
	public String getIdArg()  throws Exception {
		String rv=null;
		if ( idType==ID_TYPE_MEDIAFILE)
			rv="MediaFileId=";
		else if ( idType==ID_TYPE_AIRING)
			rv="AiringId=";
		else 
			return null;
		return rv+Integer.toString(id);
		
	}

    public String getIdArgName()  throws Exception {
        String rv=null;
        if ( idType==ID_TYPE_MEDIAFILE)
            rv="MediaFileId";
        else if ( idType==ID_TYPE_AIRING)
            rv="AiringId";
        else 
            return null;
        return rv;
        
    }
}
