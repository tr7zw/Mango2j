package dev.tr7zw.mango2j.controller;

import dev.tr7zw.mango2j.*;
import dev.tr7zw.mango2j.db.*;
import dev.tr7zw.mango2j.util.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.*;
import org.springframework.ui.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;

@Controller
public class LibraryController {

    @Autowired
    private TitleRepository titleRepo;
    @Autowired
    private ChapterRepository chapterRepo;
    @Autowired
    private Settings settings;
    @Autowired
    private StatusUtil statusUtil;

    private record TitleStats(
            Title title,
            Integer totalViews,
            Integer chapterCount,
            String newestChapterAge,
            Instant newestChapterTime
    ) {
    }

    // todo: Job that pre-calculates these stats and stores it in the Title entity?
    private TitleStats calculateTitleStats(Title title) {
        int totalViews = title.getTotalViews() == null ? 0 : title.getTotalViews();
        int chapterCount = title.getChapterCount() == null ? 0 : title.getChapterCount();
        Instant newestTime = title.getNewestChapterTime();

        String newestChapterAge = DateFormatUtil.formatTimeAgo(newestTime);
        return new TitleStats(title, totalViews, chapterCount, newestChapterAge, newestTime);
    }

    private List<TitleStats> sortTitles(List<Title> titles, String orderBy) {
        List<TitleStats> statsForAllTitles = titles.stream()
                .map(this::calculateTitleStats)
                .collect(Collectors.toList());

        switch (orderBy.toUpperCase()) {
            case "VIEWS":
                statsForAllTitles.sort((a, b) ->
                        Integer.compare(b.totalViews, a.totalViews));
                break;
            case "CHAPTERS":
                statsForAllTitles.sort((a, b) ->
                        Integer.compare(b.chapterCount, a.chapterCount));
                break;
            case "NEWEST":
                statsForAllTitles.sort((a, b) -> {
                    if (a.newestChapterTime == null && b.newestChapterTime == null) return 0;
                    if (a.newestChapterTime == null) return 1;
                    if (b.newestChapterTime == null) return -1;
                    return b.newestChapterTime.compareTo(a.newestChapterTime);
                });
                break;
            case "NAME":
            default:
                statsForAllTitles.sort((a, b) ->
                        a.title.getName().compareToIgnoreCase(b.title.getName()));
                break;
        }

        return statsForAllTitles;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("scanStatus", statusUtil.getScanStatus());
        List<Chapter> chapters = chapterRepo.findTop100ByOrderByIdDesc();
        chapters.sort((a, b) -> Integer.compare(b.getId(), a.getId())); // newest to oldest
        model.addAttribute("chapters", chapters);
        model.addAttribute("totalChapters", chapterRepo.count());

        // Sort collections by views (most viewed first) for home page
        List<Title> titles = titleRepo.findByPath(settings.getBaseDir().getPath());
        List<TitleStats> titleStats = sortTitles(titles, "VIEWS");
        model.addAttribute("titleStats", titleStats);

        // Keep original titles for thumbnail generation
        model.addAttribute("titles", titles);
        model.addAttribute("chapterThumbnails", generateThumbnails(titles));

        model.addAttribute("name", "Latest");
        // Return the name of the Thymeleaf template without the extension
        return "home";
    }

    @GetMapping("/library")
    public String home(Model model,
                       @RequestParam(name = "collectionOrderBy", defaultValue = "VIEWS") String collectionOrderBy,
                       @RequestParam(name = "orderBy", defaultValue = "NEWEST") String orderBy) {
        model.addAttribute("scanStatus", statusUtil.getScanStatus());
        model.addAttribute("collectionOrderBy", collectionOrderBy.toUpperCase());
        model.addAttribute("orderBy", orderBy.toUpperCase());

        List<Title> titles = titleRepo.findByPath(settings.getBaseDir().getPath());
        List<TitleStats> titleStats = sortTitles(titles, collectionOrderBy);
        model.addAttribute("titleStats", titleStats);

        List<Chapter> chapters = chapterRepo.findByPath(settings.getBaseDir().getPath());
        switch (orderBy.toUpperCase()) {
            case "NAME":
                chapters.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                break;
            case "VIEWS":
                chapters.sort((a, b) -> Integer.compare(b.getViews() == null ? 0 : b.getViews(),
                        a.getViews() == null ? 0 : a.getViews()));
                break;
            case "PAGES":
                chapters.sort((a, b) -> Integer.compare(b.getPageCount() == null ? 0 : b.getPageCount(),
                        a.getPageCount() == null ? 0 : a.getPageCount()));
                break;
            case "LASTVIEWED":
                chapters.sort((a, b) -> {
                    if (a.getLastView() == null && b.getLastView() == null)
                        return 0;
                    if (a.getLastView() == null)
                        return 1;
                    if (b.getLastView() == null)
                        return -1;
                    return b.getLastView().compareTo(a.getLastView());
                });
                break;
            case "OLDESTVIEWED":
                chapters.sort((a, b) -> {
                    if (a.getLastView() == null && b.getLastView() == null)
                        return 0;
                    if (a.getLastView() == null)
                        return -1;
                    if (b.getLastView() == null)
                        return 1;
                    return a.getLastView().compareTo(b.getLastView());
                });
                break;
            default:
                chapters.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
                break;
        }
        model.addAttribute("chapters", chapters);
        model.addAttribute("titles", titles);
        model.addAttribute("chapterThumbnails", generateThumbnails(titles));

        return "library";
    }

    @GetMapping("/library/{id}")
    public String libraryDir(@PathVariable Integer id,
                             @RequestParam(name = "collectionOrderBy", defaultValue = "VIEWS") String collectionOrderBy,
                             @RequestParam(name = "orderBy", defaultValue = "NEWEST") String orderBy, Model model) {
        Title title = titleRepo.getReferenceById(id);
        Title parent = titleRepo.findByFullPath(new File(title.getFullPath()).getParent());
        if (parent != null) {
            model.addAttribute("back", parent.getId());
        }
        model.addAttribute("scanStatus", statusUtil.getScanStatus());
        model.addAttribute("collectionOrderBy", collectionOrderBy.toUpperCase());
        model.addAttribute("orderBy", orderBy.toUpperCase());

        List<Title> titles = titleRepo.findByPath(title.getFullPath());
        List<TitleStats> titleStats = sortTitles(titles, collectionOrderBy);
        model.addAttribute("titleStats", titleStats);

        List<Chapter> chapters = chapterRepo.findByPath(title.getFullPath());
        switch (orderBy.toUpperCase()) {
            case "NAME":
                chapters.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                break;
            case "VIEWS":
                chapters.sort((a, b) -> Integer.compare(b.getViews() == null ? 0 : b.getViews(),
                        a.getViews() == null ? 0 : a.getViews()));
                break;
            case "PAGES":
                chapters.sort((a, b) -> Integer.compare(b.getPageCount() == null ? 0 : b.getPageCount(),
                        a.getPageCount() == null ? 0 : a.getPageCount()));
                break;
            case "LASTVIEWED":
                chapters.sort((a, b) -> {
                    if (a.getLastView() == null && b.getLastView() == null)
                        return 0;
                    if (a.getLastView() == null)
                        return 1;
                    if (b.getLastView() == null)
                        return -1;
                    return b.getLastView().compareTo(a.getLastView());
                });
                break;
            case "OLDESTVIEWED":
                chapters.sort((a, b) -> {
                    if (a.getLastView() == null && b.getLastView() == null)
                        return 0;
                    if (a.getLastView() == null)
                        return -1;
                    if (b.getLastView() == null)
                        return 1;
                    return a.getLastView().compareTo(b.getLastView());
                });
                break;
            default:
                chapters.sort((a, b) -> Integer.compare(b.getId(), a.getId())); // newest to oldest
                break;
        }
        model.addAttribute("chapters", chapters);
        model.addAttribute("titles", titles);
        model.addAttribute("name", title.getName());
        model.addAttribute("path", title.getPath());
        model.addAttribute("chapterThumbnails", generateThumbnails(titles));
        // Return the name of the Thymeleaf template without the extension
        return "library";
    }

    @GetMapping("/top")
    public String top(Model model) {
        model.addAttribute("scanStatus", statusUtil.getScanStatus());
        List<Chapter> chapters = chapterRepo.findTop100ByOrderByViewsDesc();
        model.addAttribute("chapters", chapters);
        model.addAttribute("titles", new ArrayList<>());
        model.addAttribute("name", "Top Viewed");
        // Return the name of the Thymeleaf template without the extension
        return "viewcount";
    }

    @GetMapping("/bottom")
    public String bottom(Model model) {
        model.addAttribute("scanStatus", statusUtil.getScanStatus());
        List<Chapter> chapters = chapterRepo.findTop100ByOrderByViewsAscIdDesc();
        model.addAttribute("chapters", chapters);
        model.addAttribute("titles", new ArrayList<>());
        model.addAttribute("name", "Least Viewed");
        // Return the name of the Thymeleaf template without the extension
        return "viewcount";
    }

    @GetMapping("/search")
    public String search(@RequestParam(name = "query", required = false) String query, Model model) {
        List<Chapter> chapters = new ArrayList<>();
        if (query != null && !query.isBlank()) {
            chapters = chapterRepo.findAll(ChapterRepository.descriptionMatches(query));
            model.addAttribute("name", "Search results for: " + query);
            if (chapters.isEmpty()) {
                Page<Chapter> result = chapterRepo.findAll(ChapterRepository.descriptionRankedSearch(query), PageRequest.of(0, 20));
                chapters = result.getContent();
                model.addAttribute("name", "Closest search results for: " + query);
            }

        } else {
            model.addAttribute("name", "Search");
        }
        model.addAttribute("scanStatus", statusUtil.getScanStatus());
        model.addAttribute("chapters", chapters);
        model.addAttribute("query", query);
        model.addAttribute("titles", new ArrayList<>());
        // Return the name of the Thymeleaf template without the extension
        return "search";
    }

    private Map<Integer, Integer> generateThumbnails(List<Title> titles) {
        Map<Integer, Integer> map = new HashMap<>();
        titles.forEach(t -> map.put(t.getId(), findThumbnail(t)));
        return map;
    }

    private Integer findThumbnail(Title title) {
        Optional<Chapter> chapter = chapterRepo.findByPath(title.getFullPath()).stream()
                .filter(c -> c.getThumbnail() != null).findAny();
        if (chapter.isPresent()) {
            return chapter.get().getId();
        }
        for (Title other : titleRepo.findByPath(title.getFullPath())) {
            Integer id = findThumbnail(other);
            if (id != null) {
                return id;
            }
        }
        return null;
    }

    @GetMapping("/title/{id}/info")
    @ResponseBody
    public ResponseEntity<String> titleInfo(@PathVariable Integer id) {
        Title title = titleRepo.getReferenceById(id);

        String html = String.format("""
            <div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4" id="title-info-modal" onclick="if(event.target.id === 'title-info-modal') document.getElementById('title-info-modal').remove()">
                <div class="bg-surface-container rounded-2xl max-w-md w-full max-h-[80vh] overflow-y-auto shadow-lg">
                    <!-- Header -->
                    <div class="sticky top-0 bg-surface-container-high border-b border-surface-container-highest p-6 flex justify-between items-center">
                        <h2 class="text-xl font-bold font-headline text-on-surface">Collection Info</h2>
                        <button class="text-on-surface/60 hover:text-on-surface transition-colors" onclick="document.getElementById('title-info-modal').remove()">
                            <span class="material-symbols-outlined">close</span>
                        </button>
                    </div>

                    <!-- Content -->
                    <div class="p-6 space-y-4">
                        <!-- Collection Name -->
                        <div>
                            <p class="text-xs font-bold text-on-surface/60 uppercase tracking-widest mb-1">Collection Name</p>
                            <p class="text-lg font-headline font-bold text-on-surface">%s</p>
                        </div>

                        <!-- Folder Path -->
                        <div>
                            <p class="text-xs font-bold text-on-surface/60 uppercase tracking-widest mb-1">Folder Path</p>
                            <p class="text-sm text-on-surface/80 break-all font-mono">%s</p>
                        </div>

                        <!-- Stats Grid -->
                        <div class="grid grid-cols-3 gap-3">
                            <div class="bg-surface-container-low rounded-lg p-3">
                                <p class="text-xs font-bold text-on-surface/60 uppercase tracking-widest mb-2 truncate">Total Views</p>
                                <p class="text-2xl font-bold font-headline text-primary">%d</p>
                            </div>
                            <div class="bg-surface-container-low rounded-lg p-3">
                                <p class="text-xs font-bold text-on-surface/60 uppercase tracking-widest mb-2 truncate">Chapters</p>
                                <p class="text-2xl font-bold font-headline text-secondary">%d</p>
                            </div>
                            <div class="bg-surface-container-low rounded-lg p-3">
                                <p class="text-xs font-bold text-on-surface/60 uppercase tracking-widest mb-2 truncate">Size</p>
                                <p class="text-lg font-bold font-headline text-tertiary">%s</p>
                            </div>
                        </div>

                        <!-- Newest Chapter -->
                        %s

                        <!-- Quick Actions -->
                        <div class="pt-4 border-t border-surface-container-highest space-y-2">
                            <a href="/library/%d" class="block w-full px-4 py-2 bg-primary text-on-primary font-bold rounded text-sm text-center transition-all hover:bg-primary-container">
                                Open Collection
                            </a>
                            <button onclick="document.getElementById('title-info-modal').remove()" class="w-full px-4 py-2 bg-surface-container-high text-on-surface font-bold rounded text-sm transition-all hover:bg-surface-container-highest">
                                Close
                            </button>
                        </div>
                    </div>
                </div>
            </div>
            """,
            FormatUtil.escapeHtml(title.getName()),
            FormatUtil.escapeHtml(title.getFullPath()),
            title.getTotalViews() != null ? title.getTotalViews() : 0,
            title.getChapterCount() != null ? title.getChapterCount() : 0,
            title.getFileSize() != null ? FormatUtil.formatFileSize(title.getFileSize()) : "0 B",
            title.getNewestChapterTime() != null ? String.format("<div><p class=\"text-xs font-bold text-on-surface/60 uppercase tracking-widest mb-1\">Newest Chapter</p><p class=\"text-sm text-on-surface/80\">%s</p></div>", FormatUtil.escapeHtml(DateFormatUtil.formatTimeAgo(title.getNewestChapterTime()))) : "",
            title.getId()
        );

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

}