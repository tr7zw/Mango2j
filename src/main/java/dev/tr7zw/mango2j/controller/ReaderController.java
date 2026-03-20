package dev.tr7zw.mango2j.controller;

import dev.tr7zw.mango2j.db.*;
import dev.tr7zw.mango2j.service.*;
import dev.tr7zw.mango2j.util.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.ui.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

import java.io.*;
import java.time.*;
import java.util.*;

@Controller
public class ReaderController {

    private final FileService fileService;
    private final TitleRepository titleRepo;
    private final ChapterRepository chapterRepo;
    private final MoveTargetService moveTargetService;
    private StatusUtil statusUtil;

    @Autowired
    public ReaderController(FileService fileService, TitleRepository titleRepo, ChapterRepository chapterRepo,
                            MoveTargetService moveTargetService, StatusUtil statusUtil) {
        this.fileService = fileService;
        this.titleRepo = titleRepo;
        this.chapterRepo = chapterRepo;
        this.moveTargetService = moveTargetService;
        this.statusUtil = statusUtil;
    }

    @GetMapping("/reader/{id}")
    public String home(@PathVariable Integer id, Model model) {
        // Add necessary attributes to the model
        model.addAttribute("mode", "continuous");
        Chapter chapter = chapterRepo.getReferenceById(id);
        chapter.setViews((chapter.getViews() == null ? 0 : chapter.getViews()) + 1);
        chapter.setLastView(Instant.now());
        chapterRepo.save(chapter);
        Title title = titleRepo.findByFullPath(chapter.getPath());
        try (ChapterWrapper chapterWrapper = fileService.getChapterWrapper(new File(chapter.getFullPath()).toPath())) {
            model.addAttribute("items", chapterWrapper.getFilesTyped());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        model.addAttribute("titleid", "123");
        model.addAttribute("entryid", id);
        model.addAttribute("page_idx", 0);
        model.addAttribute("margin", 0);
        model.addAttribute("base_url", "http://localhost:8080");
        model.addAttribute("exit_url", "/library/" + title.getId());
        model.addAttribute("delete_url", "/admin/delete/" + chapter.getId());
        model.addAttribute("scanStatus", statusUtil.getScanStatus());

        List<MoveTarget> moveTargets = new ArrayList<>();
        for (int i = 0; i < moveTargetService.getMoveTargets().size(); i++) {
            File target = moveTargetService.getMoveTargets().get(i);
            moveTargets.add(new MoveTarget(target.getName(), "/admin/move/" + id + "/" + i));
        }
        model.addAttribute("moveTargets", moveTargets);

        return "reader";
    }

    @GetMapping("/chapter/{id}/info")
    @ResponseBody
    public ResponseEntity<String> chapterInfo(@PathVariable Integer id) {
        Chapter chapter = chapterRepo.getReferenceById(id);
        Title title = titleRepo.findByFullPath(chapter.getPath());

        String html = String.format("""
            <div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4" id="chapter-info-modal" onclick="if(event.target.id === 'chapter-info-modal') document.getElementById('chapter-info-modal').remove()">
                <div class="bg-surface-container rounded-2xl max-w-md w-full max-h-[80vh] overflow-y-auto shadow-lg">
                    <!-- Header -->
                    <div class="sticky top-0 bg-surface-container-high border-b border-surface-container-highest p-6 flex justify-between items-center">
                        <h2 class="text-xl font-bold font-headline text-on-surface">Chapter Info</h2>
                        <button class="text-on-surface/60 hover:text-on-surface transition-colors" onclick="document.getElementById('chapter-info-modal').remove()">
                            <span class="material-symbols-outlined">close</span>
                        </button>
                    </div>

                    <!-- Content -->
                    <div class="p-6 space-y-4">
                        <!-- Chapter Name -->
                        <div>
                            <p class="text-xs font-bold text-on-surface/60 uppercase tracking-widest mb-1">Chapter Name</p>
                            <p class="text-lg font-headline font-bold text-on-surface">%s</p>
                        </div>

                        <!-- Folder Path -->
                        <div>
                            <p class="text-xs font-bold text-on-surface/60 uppercase tracking-widest mb-1">Folder Path</p>
                            <p class="text-sm text-on-surface/80 break-all font-mono">%s</p>
                        </div>

                        <!-- Collection -->
                        <div>
                            <p class="text-xs font-bold text-on-surface/60 uppercase tracking-widest mb-1">Collection</p>
                            <p class="text-sm text-on-surface/80">%s</p>
                        </div>

                        <!-- Views -->
                        <div class="grid grid-cols-2 gap-4">
                            <div class="bg-surface-container-low rounded-lg p-3">
                                <p class="text-xs font-bold text-on-surface/60 uppercase tracking-widest mb-2">Views</p>
                                <p class="text-2xl font-bold font-headline text-primary">%d</p>
                            </div>
                            <div class="bg-surface-container-low rounded-lg p-3">
                                <p class="text-xs font-bold text-on-surface/60 uppercase tracking-widest mb-2">Pages</p>
                                <p class="text-2xl font-bold font-headline text-secondary">%s</p>
                            </div>
                        </div>

                        <!-- Last Viewed -->
                        %s

                        <!-- Last Modified -->
                        %s

                        <!-- File Size -->
                        %s

                        <!-- Description -->
                        %s

                        <!-- Quick Actions -->
                        <div class="pt-4 border-t border-surface-container-highest space-y-2">
                            <a href="/reader/%d" class="block w-full px-4 py-2 bg-primary text-on-primary font-bold rounded text-sm text-center transition-all hover:bg-primary-container">
                                Open Chapter
                            </a>
                            <button onclick="document.getElementById('chapter-info-modal').remove()" class="w-full px-4 py-2 bg-surface-container-high text-on-surface font-bold rounded text-sm transition-all hover:bg-surface-container-highest">
                                Close
                            </button>
                        </div>
                    </div>
                </div>
            </div>
            """,
            FormatUtil.escapeHtml(chapter.getName()),
            FormatUtil.escapeHtml(chapter.getFullPath()),
            title != null ? FormatUtil.escapeHtml(title.getName()) : "Unknown",
            chapter.getViews() != null ? chapter.getViews() : 0,
            chapter.getPageCount() != null ? chapter.getPageCount() : "N/A",
            chapter.getLastView() != null ? String.format("<div><p class=\"text-xs font-bold text-on-surface/60 uppercase tracking-widest mb-1\">Last Viewed</p><p class=\"text-sm text-on-surface/80\">%s</p></div>", FormatUtil.escapeHtml(chapter.getLastViewFormatted())) : "",
            chapter.getLastModified() != null ? String.format("<div><p class=\"text-xs font-bold text-on-surface/60 uppercase tracking-widest mb-1\">Last Modified</p><p class=\"text-sm text-on-surface/80\">%s</p></div>", FormatUtil.escapeHtml(DateFormatUtil.formatTimeAgo(chapter.getLastModified()))) : "",
            chapter.getFileSize() != null ? String.format("<div><p class=\"text-xs font-bold text-on-surface/60 uppercase tracking-widest mb-1\">File Size</p><p class=\"text-sm text-on-surface/80\">%s</p></div>", FormatUtil.formatFileSize(chapter.getFileSize())) : "",
            chapter.getDescription() != null ? String.format("<div><p class=\"text-xs font-bold text-on-surface/60 uppercase tracking-widest mb-1\">Description</p><p class=\"text-sm text-on-surface/80 whitespace-pre-wrap break-words\">%s</p></div>", FormatUtil.escapeHtml(chapter.getDescription())) : "",
            chapter.getId()
        );

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }


    private record MoveTarget(String name, String url) {
    }

}
