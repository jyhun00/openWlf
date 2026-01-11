# OpenWLF - Open Watchlist Filtering System

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Enterprise-grade watchlist filtering system for financial compliance. Screens customers against OFAC, UN, EU, and other regulatory watchlists in real-time.

## 🎯 Project Overview

OpenWLF is a portfolio project demonstrating enterprise-level architecture for financial compliance systems. It implements real-time customer screening against multiple watchlists with explainable AI principles.

### Key Features

- **Real-time API Filtering**: RESTful API for instant customer screening
- **Multi-source Watchlists**: Support for OFAC, UN, EU, and custom lists
- **Intelligent Matching**: 
  - Exact name matching
  - Fuzzy matching (Levenshtein distance)
  - Alias checking
  - Date of birth verification
  - Nationality matching
- **🆕 Advanced Matching Algorithms**:
  - Soundex (발음 기반 - Robert ≈ Rupert)
  - Double Metaphone (다국어 발음 - Muhammad ≈ Mohammed)
  - Jaro-Winkler (이름 특화 유사도)
  - N-Gram (부분 문자열 매칭)
  - Korean Name Matching (한글 초성/자모 매칭)
  - Composite (복합 알고리즘)
- **Risk Scoring**: Weighted scoring system with configurable thresholds
- **Explainability**: Detailed explanations for regulatory compliance
- **Audit Trail**: Complete filtering history for compliance reporting
- **OpenAPI Documentation**: Interactive Swagger UI

## 🏗️ Architecture

```
openWLF/
├── core-module/          # Business logic (Normalization, RuleEngine, Scoring)
├── data-module/          # Data layer (Entities, Repositories)
├── batch-module/         # Batch processing (Future: Daily screening)
└── api-module/           # REST API & Swagger documentation
```

### Technology Stack

- **Framework**: Spring Boot 4.0.1
- **Build Tool**: Gradle
- **Database**: H2 (Development), PostgreSQL/MySQL (Production-ready)
- **API Documentation**: SpringDoc OpenAPI 3.0
- **Testing**: JUnit 5, MockMvc

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Gradle 9.2+

### Running the Application

```bash
# Clone the repository
git clone https://github.com/yourusername/openWLF.git
cd openWLF

# Build the project
./gradlew clean build

# Run the application
./gradlew :api-module:bootRun
```

The application will start on `http://localhost:8080`

### Accessing Services

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **H2 Console**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:watchlistdb`
  - Username: `sa`
  - Password: (empty)

## 📡 API Usage

### Filter Customer

**Endpoint**: `POST /api/filter/customer`

**Request**:
```json
{
  "name": "John Smith",
  "dateOfBirth": "1975-05-15",
  "nationality": "US",
  "customerId": "CUST-12345"
}
```

**Response**:
```json
{
  "alert": true,
  "score": 100.0,
  "matchedRules": [
    {
      "ruleName": "EXACT_NAME_MATCH",
      "ruleType": "NAME",
      "score": 100.0,
      "matchedValue": "JOHN SMITH",
      "targetValue": "JOHN SMITH",
      "description": "Exact name match found"
    }
  ],
  "explanation": "⚠️ ALERT: High-risk match detected...",
  "customerInfo": {
    "name": "John Smith",
    "dateOfBirth": "1975-05-15",
    "nationality": "US",
    "customerId": "CUST-12345"
  }
}
```

### cURL Example

```bash
curl -X POST http://localhost:8080/api/filter/customer \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Smith",
    "dateOfBirth": "1975-05-15",
    "nationality": "US"
  }'
```

## 🧪 Testing

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :core-module:test

# Generate test coverage report
./gradlew jacocoTestReport
```

## 📊 Scoring System

### Rule Weights

| Rule Type | Max Score | Description |
|-----------|-----------|-------------|
| Exact Name Match | 100 | Perfect name match |
| Fuzzy Name Match | 80 | High similarity (>80%) |
| Alias Match | 90 | Matches known alias |
| Date of Birth | 50 | Exact DOB match |
| Nationality | 30 | Matching nationality |

### Thresholds

- **Alert Threshold**: 70+ (Block transaction)
- **Review Threshold**: 50-69 (Manual review required)
- **Low Risk**: <50 (Proceed normally)

## 🔧 Configuration

Edit `api-module/src/main/resources/application.yml`:

```yaml
watchlist:
  threshold:
    alert: 70.0    # Alert threshold
    review: 50.0   # Review threshold
```

## 📈 Sample Data

The system initializes with sample watchlist entries including:

- OFAC SDN List entries
- UN Sanctions List entries
- EU Sanctions List entries
- Test entries for development

## 🎓 Learning Outcomes

This project demonstrates:

1. **Multi-module Architecture**: Clean separation of concerns
2. **Domain-Driven Design**: Clear domain models and business logic
3. **RESTful API Design**: Best practices for API development
4. **Testing Strategy**: Unit and integration tests
5. **Documentation**: OpenAPI/Swagger integration
6. **Financial Compliance**: Real-world AML/KYC workflows

## 🔮 Future Enhancements

- [ ] Spring Batch integration for daily screening
- [ ] Machine Learning-based scoring optimization
- [ ] Multi-language support for international names
- [ ] Real-time watchlist updates from external sources
- [x] ~~Advanced fuzzy matching algorithms (Soundex, Metaphone)~~ ✅ Completed!
- [ ] Performance optimization for large-scale screening
- [ ] Kubernetes deployment configuration
- [ ] CI/CD pipeline with GitHub Actions

## 🧬 Advanced Matching Algorithms

OpenWLF now includes sophisticated name matching algorithms for improved accuracy:

### Available Algorithms

| Algorithm | Description | Best For | Example |
|-----------|-------------|----------|---------|
| **Soundex** | English phonetic encoding | English names | Robert ≈ Rupert |
| **Double Metaphone** | Enhanced phonetic matching | Multi-lingual names | Muhammad ≈ Mohammed |
| **Jaro-Winkler** | String similarity with prefix weight | Typo detection | Jon Smith ≈ John Smith |
| **N-Gram** | Substring-based matching | Spelling variations | Anderson ≈ Andersen |
| **Korean** | Korean Chosung/Jamo matching | Korean names | 김철수 ≈ 김창수 (ㄱㅊㅅ) |
| **Composite** | Weighted combination of all | Maximum accuracy | All combined |

### Test Matching API

```bash
# Test matching algorithms
curl -X POST http://localhost:8080/api/matching/test \
  -H "Content-Type: application/json" \
  -d '{
    "name1": "Muhammad Ali",
    "name2": "Mohammed Ali"
  }'
```

**Response:**
```json
{
  "name1": "Muhammad Ali",
  "name2": "Mohammed Ali",
  "soundex": { "matched": true, "similarity": 1.0 },
  "metaphone": { "matched": true, "similarity": 1.0 },
  "jaroWinkler": { "tokenSimilarity": 0.89, "matched": true },
  "composite": { 
    "compositeScore": 0.92, 
    "highConfidenceMatch": true 
  },
  "summary": "⚠️ 높은 유사도 감지! (종합 92%) - 매칭 알고리즘: 발음(Soundex), 발음(Metaphone), Jaro-Winkler"
}
```

### Korean Name Matching Example

```bash
curl -X POST http://localhost:8080/api/matching/test \
  -H "Content-Type: application/json" \
  -d '{
    "name1": "김철수",
    "name2": "김창수"
  }'
```

**Response includes:**
- Chosung extraction: ㄱㅊㅅ
- Chosung matching: true
- Korean similarity score

## 📝 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 👤 Author

**AML Compliance Team**
- Portfolio Project
- Contact: compliance@openwlf.com

## 🙏 Acknowledgments

- Inspired by real-world financial compliance systems
- Built for educational and portfolio purposes
- Not intended for production use without proper compliance review

---

⚠️ **Disclaimer**: This is a portfolio/educational project. For production use in financial institutions, proper regulatory compliance review and testing is required.
