package aml.openwlf.batch.parser;

import aml.openwlf.batch.parser.model.ParsedSanctionsData;

import java.io.InputStream;
import java.util.List;

/**
 * 제재 리스트 XML 파서 인터페이스
 */
public interface SanctionsXmlParser {
    
    /**
     * XML 입력 스트림을 파싱하여 제재 데이터 리스트 반환
     *
     * @param inputStream XML 입력 스트림
     * @return 파싱된 제재 데이터 리스트
     * @throws Exception 파싱 실패 시
     */
    List<ParsedSanctionsData> parse(InputStream inputStream) throws Exception;
    
    /**
     * 파서가 처리하는 데이터 소스 이름 반환
     *
     * @return 데이터 소스 이름 (예: "OFAC", "UN")
     */
    String getSourceFile();
}
