package com.playsphere.event;
import java.util.List;import org.springframework.data.jpa.repository.JpaRepository;
public interface MatchRepository extends JpaRepository<Match,String>{List<Match> findByEventIdOrderByScheduledAt(String eventId);}
