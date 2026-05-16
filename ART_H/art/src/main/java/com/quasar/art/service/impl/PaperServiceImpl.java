package com.quasar.art.service.impl;


import com.quasar.art.dto.PageDTO;
import com.quasar.art.entity.Paper.Paper;
import com.quasar.art.entity.Paper.PaperAiAnalysis;
import com.quasar.art.entity.Paper.PaperRelationship;
import com.quasar.art.entity.Paper.ReviewTask;
import com.quasar.art.repository.Paper.PaperAiAnalysisRepository;
import com.quasar.art.repository.Paper.PaperRelationshipRepository;
import com.quasar.art.repository.Paper.PaperRepository;
import com.quasar.art.repository.Paper.ReviewTaskRepository;
import com.quasar.art.service.PaperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaperServiceImpl implements PaperService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaperServiceImpl.class);

    @Autowired
    private PaperRepository paperRepository;

    @Autowired
    private RestTemplate restTemplate;

    // 🌟 把你的 AI 解析 Repository 注入进来
    @Autowired
    private PaperAiAnalysisRepository aiAnalysisRepository;

    @Autowired
    private ReviewTaskRepository reviewTaskRepository;

    @Autowired
    private PaperRelationshipRepository paperRelationshipRepository;
    
    @Value("${ai.python.api.url}")
    private String pythonApiUrlConfig;

    // 从 application.properties 里读取配置的相对路径 ("./paper")
    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public List<Paper> uploadPaper(MultipartFile[] files, Long userId) {
        if (files == null || files.length == 0) {
            throw new RuntimeException("上传的 PDF 文件不能为空");
        }

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath)) {
            try { Files.createDirectories(uploadPath); } catch (IOException e) {
                throw new RuntimeException("无法创建上传目录", e);
            }
        }

        List<Paper> savedPapers = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            try {
                String originalFilename = file.getOriginalFilename();
                String fileExtension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }

                String newFileName = UUID.randomUUID().toString() + fileExtension;
                Path targetLocation = uploadPath.resolve(newFileName);
                file.transferTo(targetLocation.toFile());

                Paper paper = new Paper();
                paper.setUserId(userId);
                paper.setTitle(originalFilename);
                paper.setFilePath(newFileName);
                paper.setParseStatus(0);
                savedPapers.add(paperRepository.save(paper));
            } catch (IOException ex) {
                log.error("文件 {} 保存失败: {}", file.getOriginalFilename(), ex.getMessage());
            }
        }

        if (savedPapers.isEmpty()) {
            throw new RuntimeException("所有文件上传失败，请检查文件和目录权限");
        }
        return savedPapers;
    }
    

    @Override
    public List<Paper> getUserPapers(Long userId) {
        return paperRepository.findByUserId(userId);
    }

    @Override
    public PageDTO<Paper> getUserPapers(Long userId, int page, int size, String keyword, Integer status) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                page, size, org.springframework.data.domain.Sort.by("createdAt").descending());

        org.springframework.data.domain.Page<Paper> paperPage;
        if (keyword != null && !keyword.trim().isEmpty() && status != null) {
            paperPage = paperRepository.findByUserIdAndTitleContainingAndParseStatus(userId, keyword.trim(), status, pageable);
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            paperPage = paperRepository.findByUserIdAndTitleContaining(userId, keyword.trim(), pageable);
        } else if (status != null) {
            paperPage = paperRepository.findByUserIdAndParseStatus(userId, status, pageable);
        } else {
            paperPage = paperRepository.findByUserId(userId, pageable);
        }

        return PageDTO.of(paperPage.getContent(), page, size, paperPage.getTotalElements(), paperPage.getTotalPages());
    }

    @Override
    public int triggerBatchAnalysis(Long userId) {
        List<Paper> unparsedPapers = paperRepository.findByUserId(userId).stream()
                .filter(p -> p.getParseStatus() != null && p.getParseStatus() == 0)
                .collect(java.util.stream.Collectors.toList());

        for (Paper paper : unparsedPapers) {
            triggerAiAnalysis(paper.getId());
        }
        return unparsedPapers.size();
    }

    @Override
    public void triggerAiAnalysis(Long paperId) {
        // 1. 去数据库把这篇论文找出来
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("找不到对应的文献数据！"));

        // 🌟 核心优化 1：任务刚开始，先把状态改成 1 (解析中)，立刻存库！
        // 这样前端一刷新就能看到“努力解析中...”的状态
        paper.setParseStatus(1);
        paperRepository.save(paper);

        // 2. 开启异步线程去呼叫 Python（使用 @Async 注解的线程池）
        this.executeAiAnalysisAsync(paperId, paper);
    }

    @Async("taskExecutor")
    public void executeAiAnalysisAsync(Long paperId, Paper paper) {
        try {
            // 拼接出文件的绝对物理路径
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path targetLocation = uploadPath.resolve(paper.getFilePath());
            String absolutePath = targetLocation.toAbsolutePath().toString();

            System.out.println("🚀 [手动触发解析] 准备发送给 Python 的文件路径: " + absolutePath);

            // 组装数据并发送给 Python
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("file_path", absolutePath);

            // 发送请求
            Map response = restTemplate.postForObject(pythonApiUrlConfig, requestBody, Map.class);
            System.out.println("🎉 [手动触发解析] 收到 Python 的回信: " + response);

            // ====== 🌟 核心绝杀：解析回信并精准落库 ======
            if (response != null && (Integer) response.get("code") == 200) {
                // 1. 把 data 那一坨 Map 挖出来
                Map<String, String> data = (Map<String, String>) response.get("data");

                // 2. 查出旧数据，如果没有就 new 一个新的
                com.quasar.art.entity.Paper.PaperAiAnalysis aiData = aiAnalysisRepository.findByPaperId(paperId);
                if (aiData == null) {
                    aiData = new com.quasar.art.entity.Paper.PaperAiAnalysis();
                    aiData.setPaperId(paperId); // 只有新纪录才需要绑定 paperId
                }

                // 塞入 AI 提取的三个核心字段
                aiData.setResearchQuestion(data.get("research_question"));
                aiData.setMethodology(data.get("methodology"));
                aiData.setConclusion(data.get("conclusion"));

                // 用 Jackson 把原始 data 转成 JSON 字符串，存入你的 jsonb 字段
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                aiData.setRawAiResponse(mapper.writeValueAsString(data));

                // 保存到数据库
                aiAnalysisRepository.save(aiData);

                // 3. 🌟 成功！把主表 (Paper) 的状态改为 2 (已解析)
                paper.setParseStatus(2);
                paperRepository.save(paper);

                System.out.println("✅ [Java 线程] 完美入库！数据已成功落入 PostgreSQL，主表状态已更新为已解析！");
            } else {
                // 🌟 核心优化 2：如果 Python 没返回 200，说明内容违规或大模型抽风
                // 直接主动抛出异常，让下面的 catch 块去处理失败状态！
                throw new RuntimeException("Python 返回异常状态码：" + response);
            }
            // ========================================

        } catch (Exception e) {
            System.err.println("❌ [手动触发解析] 呼叫 Python 失败或落库异常: " + e.getMessage());
            e.printStackTrace();

            // 🌟 核心优化 3：终极兜底逻辑！只要出现任何报错（网络断了、大模型超时等）
            // 立刻把主表状态改成 3 (失败)，让前端显示红色错误并允许重试！
            paper.setParseStatus(3);
            paperRepository.save(paper);
            System.out.println("⚠️ [Java 线程] 已将文献状态标记为 3 (解析失败)，等待用户重试。");
        }
    }
    @Override
    public void deletePaper(Long paperId) {
        // 1. 先查出这篇文献，我们需要拿到它的 filePath 才知道去硬盘哪里删
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("找不到对应的文献数据！"));

        // 2. 物理毁灭：去硬盘的 paper 文件夹里把它删掉
        try {
            Path filePath = Paths.get(uploadDir).resolve(paper.getFilePath()).normalize();
            Files.deleteIfExists(filePath);
            System.out.println("🗑️ 成功删除本地物理文件: " + filePath.toString());
        } catch (IOException e) {
            // 就算硬盘上没找到文件，也不要抛异常卡死，继续删数据库就行
            System.err.println("⚠️ 物理文件删除失败或文件不存在: " + e.getMessage());
        }

        // 3. 抹除记录：从 PostgreSQL 数据库里删掉这行数据
        paperRepository.deleteById(paperId);
    }
    @Override
    public com.quasar.art.entity.Paper.PaperAiAnalysis getPaperAnalysis(Long paperId) {
        // 直接调用你的 Repository 查出数据
        return aiAnalysisRepository.findByPaperId(paperId);
    }


    @Override
    public String generateOutline(List<Long> paperIds) {
        // 1. 去主表查出用户勾选的所有文献基础信息
        List<Paper> papers = paperRepository.findAllById(paperIds);
        if (papers.isEmpty()) {
            throw new RuntimeException("未找到选中的文献记录");
        }

        // 🌟 高级技巧：直接用 Map 动态组装给 Python 的 JSON 数据，免去新建 DTO 类的烦恼！
        Map<String, Object> pythonRequest = new HashMap<>();
        List<Map<String, String>> paperInfos = new ArrayList<>();

        // 2. 遍历每一篇文献，去附表里捞出 AI 的解析结果
        for (Paper paper : papers) {
            // 防呆检查：只处理状态为 2 (已解析) 的文献
            if (paper.getParseStatus() == null || paper.getParseStatus() != 2) {
                continue; 
            }

            // 去附表查出这篇文献的 AI 提取精华
            com.quasar.art.entity.Paper.PaperAiAnalysis analysis = aiAnalysisRepository.findByPaperId(paper.getId());
            
            // 如果附表没数据，跳过这篇
            if (analysis == null) {
                System.out.println("⚠️ 警告：文献 [" + paper.getTitle() + "] 状态为已解析，但未找到附表数据！");
                continue;
            }

            // 将这篇文献的标题和三大核心要素塞进 Map
            Map<String, String> info = new HashMap<>();
            info.put("title", paper.getTitle());
            info.put("research_question", analysis.getResearchQuestion());
            info.put("methodology", analysis.getMethodology());
            info.put("conclusion", analysis.getConclusion());
            
            paperInfos.add(info);
        }

        // 再次校验：如果筛掉无效文献后，剩下的不足 2 篇，拒绝呼叫大模型
        if (paperInfos.size() < 2) {
            throw new RuntimeException("有效的已解析文献不足 2 篇，无法进行多文档交叉对比！");
        }

        // 把列表装进最终的请求体中 -> {"papers": [{...}, {...}]}
        pythonRequest.put("papers", paperInfos);

        // 3. 跨语言呼叫 Python 微服务的综述接口
        // 使用配置的 URL，替换末尾的 /parse 为 /generate-outline
        String pythonUrl = pythonApiUrlConfig.replace("/parse", "/generate-outline");
        
        try {
            System.out.println("🚀 正在呼叫 Python 微服务生成综述，共组装了 " + paperInfos.size() + " 篇文献精华...");
            
            // 发送 POST 请求，把 Map 自动转为 JSON 发过去
            org.springframework.http.ResponseEntity<Map> response = restTemplate.postForEntity(pythonUrl, pythonRequest, Map.class);
            
            // 4. 解析 Python 的返回结果
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                
                if ((Integer) body.get("code") == 200) {
                    System.out.println("✅ Python 综述生成成功！");
                    return (String) body.get("data"); // 直接把漂亮的 Markdown 字符串返回给 Vue
                } else {
                    throw new RuntimeException("Python 业务逻辑报错: " + body.get("message"));
                }
            } else {
                throw new RuntimeException("Python 服务无响应或状态码异常");
            }
        } catch (Exception e) {
            System.err.println("❌ 呼叫 Python 微服务生成综述失败: " + e.getMessage());
            throw new RuntimeException("AI 综述生成失败，请检查 Python 服务是否在 8000 端口启动");
        }
    }
    // ================== 新增的异步任务逻辑 ==================

    @Override
    public ReviewTask createReviewTask(List<Long> paperIds, Long userId) { // 🌟 加上 userId 参数
        ReviewTask task = new ReviewTask();
        // 🌟 动态赋值真实的拥护者 ID，告别写死！
        task.setUserId(userId); 
        
        // 把 List<Long> 转成逗号分隔的字符串存入数据库
        String idsStr = paperIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        task.setPaperIds(idsStr);
        task.setStatus(0);
        
        return reviewTaskRepository.save(task);
    }


// 🌟 绝杀：@Async 注解让这个方法去后台线程悄悄跑，绝对不阻塞前端！
@Async("taskExecutor") 
@Override
public void startAsyncGenerate(Long taskId, List<Long> paperIds) {
    ReviewTask task = reviewTaskRepository.findById(taskId).orElse(null);
    if (task == null) return;

    try {
        // 1. 更新状态为：1 (生成中)
        task.setStatus(1);
        reviewTaskRepository.save(task);

        System.out.println("🚀 [后台异步线程] 开始呼叫 Python 生成综述，任务ID: " + taskId);

        // 2. 🚨 这里直接复用你之前写的那个完美组装 Map 并用 RestTemplate 呼叫 Python 的逻辑！
        // 因为代码太长，我这里用一行代替，你要把你之前写的 generateOutline() 里的核心代码搬到这里来！
        String markdownResult = this.generateOutline(paperIds); // 直接调用你现成的同步方法

        // 3. 成功后，更新状态为 2，并存入生成的超长文本
        task.setContent(markdownResult);
        task.setStatus(2);
        task.setFinishedAt(LocalDateTime.now());
        reviewTaskRepository.save(task);
        
        System.out.println("✅ [后台异步线程] 综述生成完毕并已成功入库！");

    } catch (Exception e) {
        // 4. 出现任何异常（比如超时报错），更新状态为 3，并记录报错信息
        task.setStatus(3);
        task.setErrorMessage(e.getMessage());
        task.setFinishedAt(LocalDateTime.now());
        reviewTaskRepository.save(task);
        System.err.println("❌ [后台异步线程] 生成失败: " + e.getMessage());
    }
}

@Override
public List<PaperRelationship> analyzePaperRelations(List<Long> paperIds) {
    try {
        // 1. 获取文献信息
        List<Paper> papers = paperRepository.findAllById(paperIds);
        if (papers.size() < 2) {
            throw new RuntimeException("至少需要2篇文献才能分析关系");
        }

        // 2. 组装文献数据调用Python服务
        Map<String, Object> pythonRequest = new HashMap<>();
        List<Map<String, String>> paperInfos = new ArrayList<>();
        List<Paper> analyzedPapers = new ArrayList<>(); // 跟踪实际发送给 Python 的论文

        for (Paper paper : papers) {
            PaperAiAnalysis analysis = aiAnalysisRepository.findByPaperId(paper.getId());
            if (analysis != null) {
                Map<String, String> info = new HashMap<>();
                info.put("title", paper.getTitle());
                info.put("research_question", analysis.getResearchQuestion());
                info.put("methodology", analysis.getMethodology());
                info.put("conclusion", analysis.getConclusion());
                paperInfos.add(info);
                analyzedPapers.add(paper);
            }
        }

        if (paperInfos.size() < 2) {
            throw new RuntimeException("有效的已解析文献不足2篇");
        }

        pythonRequest.put("papers", paperInfos);

        // 3. 调用Python关系分析接口
        String pythonUrl = pythonApiUrlConfig.replace("/parse", "/analyze-relations");
        org.springframework.http.ResponseEntity<Map> response = restTemplate.postForEntity(
            pythonUrl, pythonRequest, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            if ((Integer) body.get("code") == 200) {
                List<Map<String, Object>> relationsData = (List<Map<String, Object>>) body.get("data");
                List<PaperRelationship> results = new ArrayList<>();

                // Python 返回的是数组索引（0,1,2...），需要映射回真实 paperId
                for (Map<String, Object> rel : relationsData) {
                    PaperRelationship relationship = new PaperRelationship();
                    Object sourceIdx = rel.get("sourcePaperId");
                    Object targetIdx = rel.get("targetPaperId");
                    int si = sourceIdx instanceof Number ? ((Number) sourceIdx).intValue() : Integer.parseInt(sourceIdx.toString());
                    int ti = targetIdx instanceof Number ? ((Number) targetIdx).intValue() : Integer.parseInt(targetIdx.toString());
                    relationship.setSourcePaperId(analyzedPapers.get(si).getId());
                    relationship.setTargetPaperId(analyzedPapers.get(ti).getId());
                    relationship.setRelationType((String) rel.get("relationType"));
                    relationship.setDescription((String) rel.get("description"));
                    results.add(relationship);
                }

                // 保存到数据库
                return paperRelationshipRepository.saveAll(results);
            }
        }

        throw new RuntimeException("关系分析服务返回异常");
    } catch (Exception e) {
        System.err.println("❌ 关系分析失败: " + e.getMessage());
        throw new RuntimeException("关系分析失败: " + e.getMessage());
    }
}

@Override
public void batchDeletePapers(List<Long> paperIds) {
    for (Long paperId : paperIds) {
        try {
            deletePaper(paperId);
        } catch (Exception e) {
            System.err.println("⚠️ 删除文献 " + paperId + " 失败: " + e.getMessage());
        }
    }
}
}