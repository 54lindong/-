package com.caseprocessor.data;

import com.caseprocessor.data.CompanyDataProcessor.CompanyAnalysisResult;
import com.caseprocessor.filehandler.CompanyExtractor.LegalPartyInfo;
import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteConfig;
import java.sql.*;
import java.util.*;
import java.nio.file.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DataStore {
    private static final String DB_PATH = "data/caseprocessor.db";
    private static DataStore instance;
    private SQLiteDataSource dataSource;

    // 单例模式获取实例
    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    private DataStore() {
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            // 确保数据目录存在
            Files.createDirectories(Paths.get("data"));
            
            // 配置SQLite数据库
            SQLiteConfig config = new SQLiteConfig();
            config.enforceForeignKeys(true);
            config.setBusyTimeout(5000);
            
            dataSource = new SQLiteDataSource(config);
            dataSource.setUrl("jdbc:sqlite:" + DB_PATH);
            
            // 创建数据库表
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                
                // 创建案件表
                stmt.execute(
                    "CREATE TABLE IF NOT EXISTS cases (" +
                    "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  case_number TEXT," +               // 案号
                    "  judgment_date TEXT," +             // 判决日期
                    "  court TEXT," +                     // 法院名称
                    "  trial_stage TEXT," +               // 审判阶段
                    "  case_type TEXT," +                 // 案件类型
                    "  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")"
                );
                
                // 创建公司信息表
                stmt.execute(
                    "CREATE TABLE IF NOT EXISTS companies (" +
                    "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  case_id INTEGER," +
                    "  company_name TEXT," +              // 公司名称
                    "  case_role TEXT," +                 // 诉讼身份
                    "  registration_status TEXT," +       // 注册状态
                    "  registration_date TEXT," +         // 注册日期
                    "  cancel_date TEXT," +               // 注销日期
                    "  has_problem BOOLEAN," +            // 是否存在问题
                    "  problem_description TEXT," +       // 问题描述
                    "  FOREIGN KEY(case_id) REFERENCES cases(id) ON DELETE CASCADE" +
                    ")"
                );
                
                // 创建文档表
                stmt.execute(
                    "CREATE TABLE IF NOT EXISTS documents (" +
                    "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "  case_id INTEGER," +
                    "  original_path TEXT," +             // 原始文件路径
                    "  stored_path TEXT," +               // 存储路径
                    "  document_type TEXT," +             // 文档类型
                    "  upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "  FOREIGN KEY(case_id) REFERENCES cases(id) ON DELETE CASCADE" +
                    ")"
                );

                // 创建索引
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_companies_case_id ON companies(case_id)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_documents_case_id ON documents(case_id)");
            }
        } catch (Exception e) {
            throw new RuntimeException("数据库初始化失败", e);
        }
    }

    // 保存案件信息
    public long saveCaseInfo(String caseNumber, String judgmentDate, 
                           String court, String trialStage, String caseType) {
        String sql = "INSERT INTO cases (case_number, judgment_date, court, " +
                    "trial_stage, case_type) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, 
                Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, caseNumber);
            pstmt.setString(2, judgmentDate);
            pstmt.setString(3, court);
            pstmt.setString(4, trialStage);
            pstmt.setString(5, caseType);
            
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("保存案件信息失败", e);
        }
        return -1;
    }

    // 保存公司信息
    public void saveCompanyInfo(long caseId, CompanyAnalysisResult company) {
        String sql = "INSERT INTO companies (case_id, company_name, case_role, " +
                    "registration_status, registration_date, cancel_date, " +
                    "has_problem, problem_description) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, caseId);
            pstmt.setString(2, company.getCompanyName());
            pstmt.setString(3, company.getCaseRole());
            pstmt.setString(4, company.getCompanyStatus());
            pstmt.setString(5, company.getRegistrationDate());
            pstmt.setString(6, company.getCancelDate());
            pstmt.setBoolean(7, company.isHasProblem());
            pstmt.setString(8, company.getProblemDescription());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("保存公司信息失败", e);
        }
    }

    // 保存文档信息
    public void saveDocument(long caseId, File file, String storedPath) {
        String sql = "INSERT INTO documents (case_id, original_path, stored_path, " +
                    "document_type) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, caseId);
            pstmt.setString(2, file.getAbsolutePath());
            pstmt.setString(3, storedPath);
            pstmt.setString(4, getFileExtension(file.getName()));
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("保存文档信息失败", e);
        }
    }

    // 获取案件历史记录
    public List<Map<String, Object>> getCaseHistory() {
        String sql = 
            "SELECT c.*, " +
            "       COUNT(DISTINCT com.id) as company_count, " +
            "       COUNT(DISTINCT doc.id) as document_count " +
            "FROM cases c " +
            "LEFT JOIN companies com ON c.id = com.case_id " +
            "LEFT JOIN documents doc ON c.id = doc.case_id " +
            "GROUP BY c.id " +
            "ORDER BY c.create_time DESC";
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", rs.getLong("id"));
                row.put("caseNumber", rs.getString("case_number"));
                row.put("judgmentDate", rs.getString("judgment_date"));
                row.put("court", rs.getString("court"));
                row.put("trialStage", rs.getString("trial_stage"));
                row.put("caseType", rs.getString("case_type"));
                row.put("createTime", rs.getTimestamp("create_time"));
                row.put("companyCount", rs.getInt("company_count"));
                row.put("documentCount", rs.getInt("document_count"));
                results.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("获取案件历史记录失败", e);
        }
        return results;
    }

    // 获取案件相关的公司信息
    public List<CompanyAnalysisResult> getCaseCompanies(long caseId) {
        String sql = "SELECT * FROM companies WHERE case_id = ?";
        List<CompanyAnalysisResult> results = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, caseId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    CompanyAnalysisResult company = new CompanyAnalysisResult(
                        rs.getString("company_name"),
                        rs.getString("case_role")
                    );
                    
                    company.setCompanyStatus(rs.getString("registration_status"));
                    company.setRegistrationDate(rs.getString("registration_date"));
                    company.setCancelDate(rs.getString("cancel_date"));
                    company.setHasProblem(rs.getBoolean("has_problem"));
                    company.setProblemDescription(rs.getString("problem_description"));
                    company.setCaseId(caseId);
                    
                    results.add(company);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("获取案件公司信息失败", e);
        }
        return results;
    }

    // 获取案件相关的文档列表
    public List<String> getCaseDocuments(long caseId) {
        String sql = "SELECT stored_path FROM documents WHERE case_id = ?";
        List<String> paths = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, caseId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    paths.add(rs.getString("stored_path"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("获取案件文档列表失败", e);
        }
        return paths;
    }

    // 删除案件及相关信息
    public void deleteCase(long caseId) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // 删除相关的公司信息
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "DELETE FROM companies WHERE case_id = ?")) {
                    pstmt.setLong(1, caseId);
                    pstmt.executeUpdate();
                }
                
                // 删除相关的文档记录
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "DELETE FROM documents WHERE case_id = ?")) {
                    pstmt.setLong(1, caseId);
                    pstmt.executeUpdate();
                }
                
                // 删除案件记录
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "DELETE FROM cases WHERE id = ?")) {
                    pstmt.setLong(1, caseId);
                    pstmt.executeUpdate();
                }
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("删除案件失败", e);
        }
    }

    // 清空所有数据
    public void clearAllData() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("DELETE FROM companies");
            stmt.execute("DELETE FROM documents");
            stmt.execute("DELETE FROM cases");
            stmt.execute("DELETE FROM sqlite_sequence");
            stmt.execute("VACUUM");
            
        } catch (SQLException e) {
            throw new RuntimeException("清空数据失败", e);
        }
    }

    // 备份数据库
    public void backup(String backupPath) {
        try {
            Files.copy(Paths.get(DB_PATH), Paths.get(backupPath), 
                StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("数据库备份失败", e);
        }
    }

    // 从备份恢复数据库
    public void restore(String backupPath) {
        try {
            Files.copy(Paths.get(backupPath), Paths.get(DB_PATH), 
                StandardCopyOption.REPLACE_EXISTING);
            
            // 重新初始化数据源
            dataSource = new SQLiteDataSource();
            dataSource.setUrl("jdbc:sqlite:" + DB_PATH);
            
        } catch (IOException e) {
            throw new RuntimeException("数据库恢复失败", e);
        }
    }

    // 获取文件扩展名
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }
}