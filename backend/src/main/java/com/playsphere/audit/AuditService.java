package com.playsphere.audit;
import java.util.List;
import org.springframework.stereotype.Service;
@Service public class AuditService{private final AuditLogRepository repo;public AuditService(AuditLogRepository repo){this.repo=repo;}public void record(String actor,String action,String targetType,String targetId,String details){repo.save(new AuditLog(actor,action,targetType,targetId,details));}public List<AuditLog> recent(){return repo.findTop200ByOrderByCreatedAtDesc();}}
