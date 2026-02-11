import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Logo } from "@/components/Logo";
import {
  ShoppingCart,
  Truck,
  Link2,
  FileText,
  BarChart3,
  Shield,
  Zap,
  Clock,
  Check,
  ArrowRight,
} from "lucide-react";

const features = [
  {
    icon: ShoppingCart,
    title: "통합 주문 수집",
    description:
      "쿠팡, 네이버 스마트스토어, 11번가, G마켓 등 주요 마켓플레이스 주문을 자동으로 수집합니다.",
  },
  {
    icon: Link2,
    title: "상품 매핑",
    description:
      "각 마켓플레이스의 상품을 자사 상품과 자동 매핑하여 재고 관리를 일원화합니다.",
  },
  {
    icon: Truck,
    title: "배송 통합 관리",
    description:
      "CJ대한통운, 한진택배 등 주요 택배사 연동으로 송장 등록부터 배송 추적까지 한 번에.",
  },
  {
    icon: FileText,
    title: "ERP 자동 연동",
    description:
      "더존, 세무사랑 등 주요 ERP 시스템과 연동하여 전표를 자동 생성합니다.",
  },
  {
    icon: BarChart3,
    title: "실시간 대시보드",
    description:
      "매출 현황, 주문 상태, 배송 현황을 한눈에 파악할 수 있는 실시간 대시보드.",
  },
  {
    icon: Shield,
    title: "안전한 데이터 관리",
    description:
      "AWS 기반 인프라와 암호화 저장으로 소중한 비즈니스 데이터를 안전하게 보호합니다.",
  },
];

const marketplaces = [
  { name: "쿠팡", color: "bg-red-500" },
  { name: "네이버 스마트스토어", color: "bg-green-500" },
  { name: "11번가", color: "bg-orange-500" },
  { name: "G마켓", color: "bg-emerald-600" },
  { name: "옥션", color: "bg-blue-600" },
  { name: "인터파크", color: "bg-purple-500" },
];

const pricingPlans = [
  {
    name: "Free",
    orderRange: "월 100건 이하",
    price: "무료",
    priceDetail: "",
    description: "서비스 체험용",
    features: [
      "월 100건까지 주문 처리",
      "마켓플레이스 1개 연동",
      "기본 대시보드",
      "이메일 지원",
    ],
    cta: "무료로 시작하기",
    popular: false,
  },
  {
    name: "스타터",
    orderRange: "월 0 ~ 1,000건",
    price: "49,000",
    priceDetail: "원 / 월",
    description: "소규모 판매자용",
    features: [
      "월 1,000건까지 주문 처리",
      "전체 마켓플레이스 연동",
      "통합 주문 대시보드",
      "배송사 연동",
      "이메일 지원",
    ],
    cta: "시작하기",
    popular: false,
  },
  {
    name: "그로스",
    orderRange: "월 1,001 ~ 5,000건",
    price: "99,000",
    priceDetail: "원 / 월",
    description: "중소 규모 판매자",
    features: [
      "월 5,000건까지 주문 처리",
      "전체 마켓플레이스 연동",
      "통합 주문 대시보드",
      "배송사 연동",
      "실시간 알림",
      "우선 기술 지원",
    ],
    cta: "시작하기",
    popular: true,
  },
  {
    name: "프로",
    orderRange: "월 5,001 ~ 15,000건",
    price: "199,000",
    priceDetail: "원 / 월",
    description: "중대형 판매자",
    features: [
      "월 15,000건까지 주문 처리",
      "전체 마켓플레이스 연동",
      "통합 주문 대시보드",
      "배송사 연동",
      "ERP 시스템 연동",
      "전담 매니저 배정",
      "API 접근",
    ],
    cta: "시작하기",
    popular: false,
  },
  {
    name: "엔터프라이즈",
    orderRange: "월 15,001 ~ 30,000건",
    price: "349,000",
    priceDetail: "원 / 월",
    description: "대형 판매자",
    features: [
      "월 30,000건까지 주문 처리",
      "전체 마켓플레이스 연동",
      "통합 주문 대시보드",
      "배송사 연동",
      "ERP 시스템 연동",
      "전담 매니저 배정",
      "커스텀 개발 지원",
      "SLA 보장",
    ],
    cta: "문의하기",
    popular: false,
  },
];

const faqs = [
  {
    question: "어떤 마켓플레이스를 지원하나요?",
    answer:
      "현재 쿠팡, 네이버 스마트스토어, 11번가, G마켓, 옥션, 인터파크를 지원하며, 위메프, 티몬 등 추가 마켓플레이스도 순차적으로 지원 예정입니다.",
  },
  {
    question: "기존 데이터를 마이그레이션할 수 있나요?",
    answer:
      "네, 기존 주문 데이터를 CSV 또는 엑셀 파일로 가져올 수 있습니다. 엔터프라이즈 플랜에서는 전담 팀이 마이그레이션을 도와드립니다.",
  },
  {
    question: "무료 플랜에서 유료 플랜으로 언제든 업그레이드할 수 있나요?",
    answer:
      "네, 언제든 업그레이드 가능합니다. 업그레이드 즉시 추가 기능이 활성화되며, 요금은 일할 계산됩니다.",
  },
  {
    question: "API를 통해 자체 시스템과 연동할 수 있나요?",
    answer:
      "프로 플랜 이상에서 REST API를 제공합니다. 상세한 API 문서와 SDK를 제공하여 쉽게 연동할 수 있습니다.",
  },
];

export default function Landing() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-background">
      {/* Navigation */}
      <header className="sticky top-0 z-50 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <nav className="mx-auto flex h-16 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
          <Logo size="md" />
          <div className="hidden items-center gap-8 md:flex">
            <a
              href="#features"
              className="text-sm text-muted-foreground transition-colors hover:text-foreground"
            >
              기능
            </a>
            <a
              href="#pricing"
              className="text-sm text-muted-foreground transition-colors hover:text-foreground"
            >
              요금제
            </a>
            <a
              href="#faq"
              className="text-sm text-muted-foreground transition-colors hover:text-foreground"
            >
              FAQ
            </a>
          </div>
          <div className="flex items-center gap-3">
            <Button variant="ghost" onClick={() => navigate("/login")}>
              로그인
            </Button>
            <Button onClick={() => navigate("/signup")}>무료로 시작하기</Button>
          </div>
        </nav>
      </header>

      <main>
        {/* Hero Section */}
        <section className="relative overflow-hidden py-20 sm:py-32">
          <div className="absolute inset-0 -z-10 bg-[radial-gradient(45%_40%_at_50%_60%,hsl(var(--primary)/0.12),transparent)]" />
          <div className="mx-auto max-w-7xl px-4 text-center sm:px-6 lg:px-8">
            <div className="mx-auto max-w-3xl">
              <div className="mb-6 inline-flex items-center gap-2 rounded-full border bg-muted/50 px-4 py-1.5 text-sm">
                <Zap className="h-4 w-4 text-primary" />
                <span>오픈마켓 판매자를 위한 올인원 솔루션</span>
              </div>
              <h1 className="mb-6 text-4xl font-bold tracking-tight sm:text-5xl lg:text-6xl">
                모든 마켓플레이스 주문을
                <br />
                <span className="text-primary">한 곳에서</span> 관리하세요
              </h1>
              <p className="mx-auto mb-10 max-w-2xl text-lg text-muted-foreground sm:text-xl">
                쿠팡, 스마트스토어, 11번가, G마켓 등 흩어진 주문을 자동으로
                수집하고, 배송 관리부터 ERP 연동까지 한 번에 처리하세요.
              </p>
              <div className="flex flex-col items-center justify-center gap-4 sm:flex-row">
                <Button
                  size="lg"
                  className="w-full sm:w-auto"
                  onClick={() => navigate("/signup")}
                >
                  14일 무료 체험 시작
                  <ArrowRight className="ml-2 h-4 w-4" />
                </Button>
                <Button
                  variant="outline"
                  size="lg"
                  className="w-full sm:w-auto"
                >
                  데모 보기
                </Button>
              </div>
            </div>

            {/* Marketplace Logos */}
            <div className="mt-16">
              <p className="mb-6 text-sm text-muted-foreground">
                주요 마켓플레이스 연동 지원
              </p>
              <div className="flex flex-wrap items-center justify-center gap-3">
                {marketplaces.map((marketplace) => (
                  <div
                    key={marketplace.name}
                    className="flex items-center gap-2 rounded-full border bg-background px-4 py-2 text-sm font-medium shadow-sm"
                  >
                    <div
                      className={`h-3 w-3 rounded-full ${marketplace.color}`}
                    />
                    {marketplace.name}
                  </div>
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* Stats Section */}
        <section className="border-y bg-muted/30 py-12">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="grid grid-cols-2 gap-8 md:grid-cols-4">
              <div className="text-center">
                <div className="text-3xl font-bold text-primary sm:text-4xl">
                  500+
                </div>
                <div className="mt-1 text-sm text-muted-foreground">
                  활성 셀러
                </div>
              </div>
              <div className="text-center">
                <div className="text-3xl font-bold text-primary sm:text-4xl">
                  1M+
                </div>
                <div className="mt-1 text-sm text-muted-foreground">
                  처리된 주문
                </div>
              </div>
              <div className="text-center">
                <div className="text-3xl font-bold text-primary sm:text-4xl">
                  99.9%
                </div>
                <div className="mt-1 text-sm text-muted-foreground">
                  서비스 가동률
                </div>
              </div>
              <div className="text-center">
                <div className="text-3xl font-bold text-primary sm:text-4xl">
                  4.8
                </div>
                <div className="mt-1 text-sm text-muted-foreground">
                  고객 만족도
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Features Section */}
        <section id="features" className="py-20 sm:py-32">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="mx-auto max-w-2xl text-center">
              <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
                강력한 기능으로 업무 효율 극대화
              </h2>
              <p className="mt-4 text-lg text-muted-foreground">
                여러 플랫폼을 오가며 낭비되는 시간을 줄이고, 핵심 비즈니스에
                집중하세요.
              </p>
            </div>

            <div className="mt-16 grid gap-8 sm:grid-cols-2 lg:grid-cols-3">
              {features.map((feature) => (
                <Card key={feature.title} className="border-2 transition-colors hover:border-primary/50">
                  <CardHeader>
                    <div className="mb-2 flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10">
                      <feature.icon className="h-6 w-6 text-primary" />
                    </div>
                    <CardTitle className="text-xl">{feature.title}</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <CardDescription className="text-base">
                      {feature.description}
                    </CardDescription>
                  </CardContent>
                </Card>
              ))}
            </div>
          </div>
        </section>

        {/* How It Works */}
        <section className="border-y bg-muted/30 py-20 sm:py-32">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="mx-auto max-w-2xl text-center">
              <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
                3단계로 시작하는 통합 관리
              </h2>
              <p className="mt-4 text-lg text-muted-foreground">
                복잡한 설정 없이 빠르게 시작할 수 있습니다.
              </p>
            </div>

            <div className="mt-16 grid gap-8 md:grid-cols-3">
              <div className="relative text-center">
                <div className="mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-full bg-primary text-2xl font-bold text-primary-foreground">
                  1
                </div>
                <h3 className="mb-2 text-xl font-semibold">계정 연결</h3>
                <p className="text-muted-foreground">
                  판매 중인 마켓플레이스 계정을 연결하세요. API 키 입력만으로
                  간편하게 연동됩니다.
                </p>
              </div>
              <div className="relative text-center">
                <div className="mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-full bg-primary text-2xl font-bold text-primary-foreground">
                  2
                </div>
                <h3 className="mb-2 text-xl font-semibold">상품 매핑</h3>
                <p className="text-muted-foreground">
                  각 마켓플레이스의 상품을 자사 상품과 매핑하여 통합
                  관리하세요.
                </p>
              </div>
              <div className="relative text-center">
                <div className="mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-full bg-primary text-2xl font-bold text-primary-foreground">
                  3
                </div>
                <h3 className="mb-2 text-xl font-semibold">자동화 시작</h3>
                <p className="text-muted-foreground">
                  주문 수집부터 배송, ERP 연동까지 모든 과정이 자동으로
                  처리됩니다.
                </p>
              </div>
            </div>
          </div>
        </section>

        {/* Pricing Section */}
        <section id="pricing" className="py-20 sm:py-32">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="mx-auto max-w-2xl text-center">
              <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
                월 주문 건수에 따른 합리적인 요금제
              </h2>
              <p className="mt-4 text-lg text-muted-foreground">
                비즈니스 규모에 맞게 선택하고, 성장에 따라 업그레이드하세요.
              </p>
            </div>

            <div className="mt-16 grid gap-6 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
              {pricingPlans.map((plan) => (
                <Card
                  key={plan.name}
                  className={`relative flex flex-col ${
                    plan.popular
                      ? "border-2 border-primary shadow-lg"
                      : "border-2"
                  }`}
                >
                  {plan.popular && (
                    <div className="absolute -top-4 left-1/2 -translate-x-1/2 rounded-full bg-primary px-4 py-1 text-sm font-medium text-primary-foreground">
                      추천
                    </div>
                  )}
                  <CardHeader className="text-center">
                    <CardTitle className="text-xl">{plan.name}</CardTitle>
                    <div className="mt-1 rounded-full bg-muted px-3 py-1 text-xs font-medium">
                      {plan.orderRange}
                    </div>
                    <CardDescription className="mt-2">{plan.description}</CardDescription>
                    <div className="mt-4">
                      <span className="text-3xl font-bold">{plan.price}</span>
                      <span className="text-sm text-muted-foreground">
                        {plan.priceDetail}
                      </span>
                    </div>
                  </CardHeader>
                  <CardContent className="flex flex-1 flex-col">
                    <ul className="mb-6 flex-1 space-y-2">
                      {plan.features.map((feature) => (
                        <li key={feature} className="flex items-start gap-2">
                          <Check className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
                          <span className="text-sm">{feature}</span>
                        </li>
                      ))}
                    </ul>
                    <Button
                      className="w-full"
                      variant={plan.popular ? "default" : "outline"}
                      onClick={() => navigate("/signup")}
                    >
                      {plan.cta}
                    </Button>
                  </CardContent>
                </Card>
              ))}
            </div>
          </div>
        </section>

        {/* Time Savings Section */}
        <section className="border-y bg-muted/30 py-20 sm:py-32">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="grid items-center gap-12 lg:grid-cols-2">
              <div>
                <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
                  매일 2시간 이상의 업무 시간 절약
                </h2>
                <p className="mt-4 text-lg text-muted-foreground">
                  여러 플랫폼을 오가며 주문을 확인하고, 엑셀로 데이터를
                  정리하고, 일일이 송장을 등록하는 반복 작업에서 벗어나세요.
                </p>
                <ul className="mt-8 space-y-4">
                  <li className="flex items-center gap-3">
                    <Clock className="h-5 w-5 text-primary" />
                    <span>주문 확인 시간 80% 단축</span>
                  </li>
                  <li className="flex items-center gap-3">
                    <Clock className="h-5 w-5 text-primary" />
                    <span>송장 등록 자동화로 실수 제로</span>
                  </li>
                  <li className="flex items-center gap-3">
                    <Clock className="h-5 w-5 text-primary" />
                    <span>정산 및 세무 처리 간소화</span>
                  </li>
                </ul>
              </div>
              <div className="rounded-2xl border-2 bg-background p-8 shadow-lg">
                <div className="space-y-6">
                  <div>
                    <div className="mb-2 flex justify-between text-sm">
                      <span>기존 방식</span>
                      <span className="text-muted-foreground">
                        하루 4시간+
                      </span>
                    </div>
                    <div className="h-4 rounded-full bg-destructive/20">
                      <div className="h-full w-full rounded-full bg-destructive" />
                    </div>
                  </div>
                  <div>
                    <div className="mb-2 flex justify-between text-sm">
                      <span>SellSync 사용</span>
                      <span className="text-primary">하루 30분</span>
                    </div>
                    <div className="h-4 rounded-full bg-primary/20">
                      <div className="h-full w-1/4 rounded-full bg-primary" />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* FAQ Section */}
        <section id="faq" className="py-20 sm:py-32">
          <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8">
            <div className="text-center">
              <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">
                자주 묻는 질문
              </h2>
              <p className="mt-4 text-lg text-muted-foreground">
                궁금한 점이 있으시면 언제든 문의해 주세요.
              </p>
            </div>

            <div className="mt-12 space-y-6">
              {faqs.map((faq) => (
                <div
                  key={faq.question}
                  className="rounded-lg border-2 bg-background p-6"
                >
                  <h3 className="font-semibold">{faq.question}</h3>
                  <p className="mt-2 text-muted-foreground">{faq.answer}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* CTA Section */}
        <section className="bg-primary py-20">
          <div className="mx-auto max-w-4xl px-4 text-center sm:px-6 lg:px-8">
            <h2 className="text-3xl font-bold tracking-tight text-primary-foreground sm:text-4xl">
              지금 바로 시작하세요
            </h2>
            <p className="mx-auto mt-4 max-w-2xl text-lg text-primary-foreground/80">
              14일 무료 체험으로 SellSync의 모든 기능을 경험해 보세요. 신용카드
              없이 시작할 수 있습니다.
            </p>
            <div className="mt-8 flex flex-col items-center justify-center gap-4 sm:flex-row">
              <Button
                size="lg"
                variant="secondary"
                className="w-full sm:w-auto"
                onClick={() => navigate("/signup")}
              >
                무료 체험 시작하기
                <ArrowRight className="ml-2 h-4 w-4" />
              </Button>
              <Button
                size="lg"
                variant="outline"
                className="w-full border-primary-foreground/30 text-primary-foreground hover:bg-primary-foreground/10 sm:w-auto"
              >
                영업팀 문의
              </Button>
            </div>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="border-t bg-muted/30 py-12">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="grid gap-8 md:grid-cols-4">
            <div>
              <Logo size="sm" />
              <p className="mt-4 text-sm text-muted-foreground">
                오픈마켓 셀러를 위한
                <br />
                올인원 주문 관리 플랫폼
              </p>
            </div>
            <div>
              <h4 className="mb-4 font-semibold">제품</h4>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li>
                  <a href="#features" className="hover:text-foreground">
                    기능
                  </a>
                </li>
                <li>
                  <a href="#pricing" className="hover:text-foreground">
                    요금제
                  </a>
                </li>
                <li>
                  <a href="#" className="hover:text-foreground">
                    업데이트
                  </a>
                </li>
              </ul>
            </div>
            <div>
              <h4 className="mb-4 font-semibold">지원</h4>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li>
                  <a href="#faq" className="hover:text-foreground">
                    FAQ
                  </a>
                </li>
                <li>
                  <a href="#" className="hover:text-foreground">
                    고객센터
                  </a>
                </li>
                <li>
                  <a href="#" className="hover:text-foreground">
                    API 문서
                  </a>
                </li>
              </ul>
            </div>
            <div>
              <h4 className="mb-4 font-semibold">법적 고지</h4>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li>
                  <a href="#" className="hover:text-foreground">
                    이용약관
                  </a>
                </li>
                <li>
                  <a href="#" className="hover:text-foreground">
                    개인정보처리방침
                  </a>
                </li>
              </ul>
            </div>
          </div>
          <div className="mt-12 border-t pt-8 text-center text-sm text-muted-foreground">
            <p>&copy; 2026 SellSync. All rights reserved.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}
